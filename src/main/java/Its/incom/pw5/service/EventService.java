package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
}
