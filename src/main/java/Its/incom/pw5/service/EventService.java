package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EventService {
    private final EventRepository eventRepository;
    private final TopicService topicService;
    private final UserService userService;
    private final SpeakerInboxRepository speakerInboxRepository;

    public EventService(EventRepository eventRepository, TopicService topicService, UserService userService, SpeakerInboxRepository speakerInboxRepository) {
        this.eventRepository = eventRepository;
        this.topicService = topicService;
        this.userService = userService;
        this.speakerInboxRepository = speakerInboxRepository;
    }

    public Event createEvent(Event event) {
        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create an event with a past date.");
        }
        // Set default status to PENDING if not provided
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.PENDING);
        }

        // Validate that at least one topic is provided
        validateTopics(event);

        // Create and initialize a new event object
        Event newEvent = new Event();
        newEvent.setStartDate(event.getStartDate());
        newEvent.setEndDate(event.getEndDate());
        newEvent.setPlace(event.getPlace());
        newEvent.setTopics(new ArrayList<>(event.getTopics())); // Ensure list copy
        newEvent.setTitle(event.getTitle());
        newEvent.setStatus(event.getStatus());
        newEvent.setMaxPartecipants(event.getMaxPartecipants());
        newEvent.setRegisterdPartecipants(0);
        newEvent.setSpeakers(new ArrayList<>());
        newEvent.setHosts(new ArrayList<>()); // TODO: Implement hosts
        newEvent.setPendingSpeakerRequests(
                event.getPendingSpeakerRequests() != null ? new ArrayList<>(event.getPendingSpeakerRequests()) : new ArrayList<>()
        );

        // Persist the Event to generate an ID
        eventRepository.addEvent(newEvent);
        if (newEvent.getId() == null) {
            throw new IllegalStateException("Failed to persist event, as the ID is null.");
        }

        // Process pending speaker requests
        processPendingSpeakerRequests(newEvent.getPendingSpeakerRequests(), newEvent.getId());

        event.setPendingSpeakerRequests(null);

        return newEvent;
    }

    public Event updateEvent(ObjectId id, Event updatedEvent, String speakerEmail) {
        // Fetch the existing event
        Event existingEvent = getExistingEvent(id);

        // Validate event dates
        validateEventDates(existingEvent, updatedEvent);

        // Update editable fields
        updateEditableFields(existingEvent, updatedEvent);

        // Process pending speaker requests properly
        if (updatedEvent.getPendingSpeakerRequests() != null && !updatedEvent.getPendingSpeakerRequests().isEmpty()) {
            List<User> newPendingRequests = new ArrayList<>();
            for (User speakerRequest : updatedEvent.getPendingSpeakerRequests()) {
                if (speakerRequest.getEmail() != null) {
                    User speaker = userService.getUserByEmail(speakerRequest.getEmail());
                    if (speaker == null) {
                        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                                .entity("User " + speakerRequest.getEmail() + " does not exist.").build());
                    }
                    addSpeakerToEvent(speaker, existingEvent.getId());
                    newPendingRequests.add(speakerRequest);
                }
            }
            // Ensure the event stores pending speaker requests
            existingEvent.setPendingSpeakerRequests(newPendingRequests);
        }

        // Handle optional speakerEmail query parameter
        if (speakerEmail != null && !speakerEmail.isBlank()) {
            User speaker = userService.getUserByEmail(speakerEmail);
            if (speaker == null) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("User " + speakerEmail + " does not exist.").build());
            }
            addSpeakerToEvent(speaker, existingEvent.getId());
        }

        // Persist updated event
        eventRepository.updateEvent(existingEvent);
        return existingEvent;
    }

    private void processPendingSpeakerRequests(List<User> pendingSpeakerRequests, ObjectId eventId) {
        if (pendingSpeakerRequests != null && !pendingSpeakerRequests.isEmpty()) {
            for (User speakerRequest : pendingSpeakerRequests) {
                try {
                    addSpeakerToEvent(speakerRequest, eventId);
                } catch (WebApplicationException e) {
                    // Log the error and propagate it to the client
                    System.err.println("Error processing speaker request for email: "
                                       + speakerRequest.getEmail() + ". Error: " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    private Event getExistingEvent(ObjectId id) {
        return eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));
    }

    private void validateTopics(Event event) {
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            throw new IllegalArgumentException("At least one topic is required.");
        }

        List<String> finalTopics = new ArrayList<>();
        for (String topicName : event.getTopics()) {
            Topic topic = topicService.findOrCreateTopic(topicName);
            if (topic != null) {
                finalTopics.add(topic.getName());
            }
            event.setTopics(new ArrayList<>(finalTopics));
        }
        event.setTopics(finalTopics);
    }

    private void validateEventDates(Event existingEvent, Event updatedEvent) {
        // Check if the event has already occurred
        if (existingEvent.getStartDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }
    }

    private void updateEditableFields(Event existingEvent, Event updatedEvent) {
        if (updatedEvent.getPlace() != null) {
            existingEvent.setPlace(updatedEvent.getPlace());
        }
        if (updatedEvent.getTopics() != null) {
            existingEvent.setTopics(updatedEvent.getTopics());
        }
        if (updatedEvent.getStartDate() != null) {
            existingEvent.setStartDate(updatedEvent.getStartDate());
        }
        if (updatedEvent.getMaxPartecipants() > 0) {
            existingEvent.setMaxPartecipants(updatedEvent.getMaxPartecipants());
        }
    }

    private void addSpeakerToEvent(User speaker, ObjectId eventId) {
        // Fetch the full user object from the database
        User fullSpeaker = userService.getUserByEmail(speaker.getEmail());
        if (fullSpeaker == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("User " + speaker.getEmail() + " does not exist.").build());
        }

        // Validate speaker role
        if (fullSpeaker.getRole() == null || !fullSpeaker.getRole().equals(Role.SPEAKER)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("User " + fullSpeaker.getEmail() + " is not a speaker.").build());
        }

        // Check for duplicates in the SpeakerInbox
        boolean duplicateRequest = speakerInboxRepository.find(
                "speakerEmail = ?1 and eventId = ?2",
                fullSpeaker.getEmail(), eventId
        ).firstResultOptional().isPresent();

        if (duplicateRequest) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Speaker request already exists for: " + fullSpeaker.getEmail()).build());
        }

        // Create a new SpeakerInbox entry with PENDING status
        SpeakerInbox inbox = new SpeakerInbox();
        inbox.setSpeakerEmail(fullSpeaker.getEmail());
        inbox.setEventId(eventId);
        inbox.setStatus(SpeakerInboxStatus.PENDING);
        speakerInboxRepository.addSpeakerInbox(inbox);

        // Log successful request
        System.out.println("Speaker request created for: " + fullSpeaker.getEmail());
    }
    public void onStartup(@Observes StartupEvent event) {
        System.out.println("Application started, running archivePastEvents now...");
        archivePastEvents();
    }
    @Scheduled(cron = "0 0 0 * * ?")
    public void archivePastEvents() {
        System.out.println("Scheduled task 'archivePastEvents' started at: " + LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // Find all CONFIRMED events where the end date has already passed
        List<Event> eventsToArchive = eventRepository.find(
                "status = ?1 and endDate < ?2", EventStatus.CONFIRMED, now
        ).list();

        if (!eventsToArchive.isEmpty()) {
            for (Event event : eventsToArchive) {
                // Archive the event
                event.setStatus(EventStatus.ARCHIVED);
                eventRepository.updateEvent(event);
                System.out.println("Archived event: " + event.getTitle());

                // Remove all related SpeakerInbox entries
                removeRelatedSpeakerInboxes(event.getId());
            }
        } else {
            System.out.println("No events to archive at this time.");
        }

        System.out.println("Scheduled task 'archivePastEvents' completed at: " + LocalDateTime.now());
    }


    private void removeRelatedSpeakerInboxes(ObjectId eventId) {
        // Fetch all related SpeakerInbox entries for the event
        List<SpeakerInbox> relatedInboxes = speakerInboxRepository.find(
                "eventId", eventId
        ).list();

        if (!relatedInboxes.isEmpty()) {
            for (SpeakerInbox inbox : relatedInboxes) {
                speakerInboxRepository.delete(inbox);
                System.out.println("Deleted SpeakerInbox entry for speaker: " + inbox.getSpeakerEmail());
            }
        } else {
            System.out.println("No SpeakerInbox entries found for event ID: " + eventId);
        }

    }

    public void checkAndBookEvent(Event event, User user) {
        // Check if the event is provided
        if (event.getId() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(event.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));

        // Check if the user has already booked the event
        if (user.getUserDetails().getBookedEvents().contains(existingEvent)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("User has already booked this event.").build());
        }

        // Check if the event is CONFIRMED or ARCHIVED
        if (existingEvent.getStatus() != EventStatus.CONFIRMED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event is not confirmed.").build());
        }

        // Check if the event has already occurred
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }

        // Check if the event is full
        if (existingEvent.getMaxPartecipants() > 0 && existingEvent.getRegisterdPartecipants() >= existingEvent.getMaxPartecipants()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event is full.").build());
        }

        // Add the user to the event
        existingEvent.setRegisterdPartecipants(existingEvent.getRegisterdPartecipants() + 1);
        user.getUserDetails().getBookedEvents().add(existingEvent);

        // Persist the updated event and user
        eventRepository.updateEvent(existingEvent);
        userService.updateUserBookedEvents(user);
    }

    public void checkAndRevokeEvent(Event event, User user) {
        // Check if the event is provided
        if (event.getId() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(event.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));

        // Check if the user has already booked the event
        if (!user.getUserDetails().getBookedEvents().contains(existingEvent)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("User has not booked this event.").build());
        }

        // Check if the event has already occurred
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }

        // Remove the user from the event
        existingEvent.setRegisterdPartecipants(existingEvent.getRegisterdPartecipants() - 1);

        // Remove the event from the user's booked events
        List<Event> bookedEvents = user.getUserDetails().getBookedEvents();
        for (Event e : bookedEvents) {
            if (e.getId().equals(existingEvent.getId())) {
                bookedEvents.remove(e);
                break;
            }
        }
        user.getUserDetails().setBookedEvents(bookedEvents);

        // Persist the updated event and user
        eventRepository.updateEvent(existingEvent);
        userService.updateUserBookedEvents(user);
    }

    public Event getEventById(Event eventId) {
        return eventRepository.findByIdOptional(eventId.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));
    }

    public List<Event> getEventsByTopic(List<String> topics) {
        return eventRepository.getEventsByTopic(topics);
    }

    public List<Event> getEventsByDate(String date) {
        LocalDateTime localDate = LocalDateTime.parse(date);
        return eventRepository.getEventsByDate(localDate);
    }

    public List<Event> getEventsBySpeaker(List<String> speakersNames) {
        List<Event> events = new ArrayList<>();
        for (String s : speakersNames) {
            List<User> usersByFullName = userService.findUserByFullName(s);
            if (usersByFullName != null) {
                for (User u : usersByFullName) {
                    if (Role.SPEAKER.equals(u.getRole())) {
                        List<Event> eventsBySpeaker = eventRepository.getEventsBySpeakerMail(u.getEmail());
                        if (eventsBySpeaker != null) {
                            events.addAll(eventsBySpeaker);
                        }
                    }
                }
            }
        }
        return events;
    }

    public List<Event> getAllEvents() {
        return eventRepository.getAllEvents();
    }
}
