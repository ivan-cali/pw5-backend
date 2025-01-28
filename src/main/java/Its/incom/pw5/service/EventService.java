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
        // Default status to PENDING if not provided
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.PENDING);
        }

        // Validate topics
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            throw new IllegalArgumentException("At least one topic is required.");
        }

        // Ensure each topic exists or create if not
        List<String> finalTopics = new ArrayList<>();
        for (String t : event.getTopics()) {
            Topic topic = topicService.findOrCreateTopic(t);
            if (topic != null) {
                finalTopics.add(topic.getName());
            }
        }
        event.setTopics(finalTopics);

        // Persist the Event to generate an ID
        eventRepository.addEvent(event);
        if (event.getId() == null) {
            throw new IllegalStateException("Failed to persist event, ID is null.");
        }

        // Validate and add SpeakerInbox entries
        if (event.getSpeakers() != null) {
            for (User speaker : event.getSpeakers()) {
                addSpeakerToEvent(speaker, event.getId());
            }
        }

        return event;
    }

    public Event updateEvent(ObjectId id, Event updatedEvent, String speakerEmail) {
        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException("Event not found", 404));

        // Check if the event has already occurred
        if (existingEvent.getDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException("Event cannot be updated because it has already occurred.", 400);
        }

        // Ensure the existing event is at least 2 weeks away
        if (existingEvent.getDate().isBefore(LocalDateTime.now().plusWeeks(2))) {
            throw new WebApplicationException("Event cannot be updated as it is less than 2 weeks away.", 400);
        }

        // Ensure the new date is at least 2 weeks away if it is being updated
        if (updatedEvent.getDate() != null && updatedEvent.getDate().isBefore(LocalDateTime.now().plusWeeks(2))) {
            throw new WebApplicationException("The new event date must be at least 2 weeks from today.", 400);
        }

        // Update editable fields
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

        // Process and add speakers if provided
        if (updatedEvent.getSpeakers() != null && !updatedEvent.getSpeakers().isEmpty()) {
            for (User speaker : updatedEvent.getSpeakers()) {
                addSpeakerToEvent(speaker, existingEvent.getId());
            }
        }

        // Process optional speakerEmail query param if provided
        if (speakerEmail != null && !speakerEmail.isBlank()) {
            User speaker = userService.getUserByEmail(speakerEmail);
            if (speaker == null) {
                throw new WebApplicationException("User " + speakerEmail + " does not exist.", 400);
            }
            addSpeakerToEvent(speaker, existingEvent.getId());
        }

        // Persist updated event
        eventRepository.updateEvent(existingEvent);
        return existingEvent;
    }

    private void addSpeakerToEvent(User speaker, ObjectId eventId) {
        // Fetch the full user object from the database
        User fullSpeaker = userService.getUserByEmail(speaker.getEmail());
        if (fullSpeaker == null) {
            throw new WebApplicationException("User " + speaker.getEmail() + " does not exist.", 400);
        }

        // Validate speaker role
        if (fullSpeaker.getRole() == null || !fullSpeaker.getRole().equals(Role.SPEAKER)) {
            String errorMessage = "User " + fullSpeaker.getEmail() + " is not a SPEAKER.";
            System.err.println(errorMessage); // Log the error
            throw new WebApplicationException(errorMessage, 400);
        }

        // Check for duplicates before persisting
        boolean duplicateRequest = speakerInboxRepository.find(
                "speakerEmail = ?1 and eventId = ?2",
                fullSpeaker.getEmail(), eventId
        ).firstResultOptional().isPresent();

        if (duplicateRequest) {
            String errorMessage = "Duplicate speaker request detected for email: "
                    + fullSpeaker.getEmail() + " and eventId: " + eventId;
            System.err.println(errorMessage); // Log the error
            throw new WebApplicationException(errorMessage, 400);
        }

        // If no duplicate, create and persist the SpeakerInbox
        SpeakerInbox inbox = new SpeakerInbox();
        inbox.setSpeakerEmail(fullSpeaker.getEmail());
        inbox.setEventId(eventId);
        inbox.setStatus(SpeakerInboxStatus.PENDING);
        speakerInboxRepository.addSpeakerInbox(inbox);

        // Log successful creation
        System.out.println("SpeakerInbox created for: " + fullSpeaker.getEmail());
        System.out.println("SpeakerInbox created for EventId: " + eventId);
    }
}
