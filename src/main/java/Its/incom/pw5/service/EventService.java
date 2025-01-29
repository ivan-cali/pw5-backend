package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EventService {

    @Inject
    EventRepository eventRepository;

    @Inject
    SpeakerInboxRepository speakerInboxRepository;

    @Inject
    TopicService topicService;

    @Inject
    UserService userService;

    public Event createEvent(Event event) {
        if (event.getDate().isBefore(LocalDateTime.now())) {
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
        newEvent.setDate(event.getDate());
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
                        throw new WebApplicationException("User not found", 404);
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
                throw new WebApplicationException("User " + speakerEmail + " does not exist.", 404);
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
                .orElseThrow(() -> new WebApplicationException("Event not found", 404));
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
        }
        event.setTopics(finalTopics);
    }

    private void validateEventDates(Event existingEvent, Event updatedEvent) {
        // Check if the event has already occurred
        if (existingEvent.getDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException("Event cannot be updated because it has already occurred.", 400);
        }

        // Ensure the existing event is at least 2 weeks away
        if (existingEvent.getDate().isBefore(LocalDateTime.now().plusWeeks(2))) {
            throw new WebApplicationException("Event cannot be updated as it is less than 2 weeks away.", 400);
        }

        // Ensure the new date is at least 2 weeks away if being updated
        if (updatedEvent.getDate() != null && updatedEvent.getDate().isBefore(LocalDateTime.now().plusWeeks(2))) {
            throw new WebApplicationException("The new event date must be at least 2 weeks from today.", 400);
        }
    }

    private void updateEditableFields(Event existingEvent, Event updatedEvent) {
        if (updatedEvent.getPlace() != null) {
            existingEvent.setPlace(updatedEvent.getPlace());
        }
        if (updatedEvent.getTopics() != null) {
            existingEvent.setTopics(updatedEvent.getTopics());
        }
        if (updatedEvent.getDate() != null) {
            existingEvent.setDate(updatedEvent.getDate());
        }
        if (updatedEvent.getMaxPartecipants() > 0) {
            existingEvent.setMaxPartecipants(updatedEvent.getMaxPartecipants());
        }
    }

    private void addSpeakerToEvent(User speaker, ObjectId eventId) {
        // Fetch the full user object from the database
        User fullSpeaker = userService.getUserByEmail(speaker.getEmail());
        if (fullSpeaker == null) {
            throw new WebApplicationException("User " + speaker.getEmail() + " does not exist.", 404);
        }

        // Validate speaker role
        if (fullSpeaker.getRole() == null || !fullSpeaker.getRole().equals(Role.SPEAKER)) {
            throw new WebApplicationException("User " + fullSpeaker.getEmail() + " is not a SPEAKER.", 400);
        }

        // Check for duplicates in the SpeakerInbox
        boolean duplicateRequest = speakerInboxRepository.find(
                "speakerEmail = ?1 and eventId = ?2",
                fullSpeaker.getEmail(), eventId
        ).firstResultOptional().isPresent();

        if (duplicateRequest) {
            throw new WebApplicationException(
                    "Duplicate speaker request detected for email: " + fullSpeaker.getEmail(), 400);
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
}
