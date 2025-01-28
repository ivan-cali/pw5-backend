package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
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
    TopicService topicService;

    public Event createEvent(Event event) {
        // Default status to PENDING if not provided
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.PENDING);
        }
        // Check that topics list isn't null or empty
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            throw new IllegalArgumentException("At least one topic is required.");
        }
        // Ensure each topic exists or create if not
        if (event.getTopics() != null) {
            List<String> finalTopics = new ArrayList<>();
            for (String t : event.getTopics()) {
                Topic topic = topicService.findOrCreateTopic(t);
                if (topic != null) {
                    finalTopics.add(topic.getName());
                }
            }
            event.setTopics(new ArrayList<>(finalTopics));
        }

        // Persist the Event
        eventRepository.addEvent(event);
        return event;
    }

    public Event updateEvent(ObjectId id, Event updatedEvent) {
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

        // Persist updated event
        eventRepository.updateEvent(existingEvent);
        return existingEvent;
    }
}
