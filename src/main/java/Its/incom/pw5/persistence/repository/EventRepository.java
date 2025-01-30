package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Event;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class EventRepository implements PanacheMongoRepository<Event> {

    public void addEvent(Event event) {
        persist(event);
    }

    public void updateEvent(Event existingEvent) {
        update(existingEvent);
    }

    public List<Event> getEventsByTopic(List<String> topic) {
        return list("topics", topic);
    }

    public List<Event> getEventsByDate(LocalDateTime date) {
        return list("date", date);
    }

    public List<Event> getEventsBySpeakerMail(String speakerMail) {
        return list("speakers.email", speakerMail);
    }

    public List<Event> getAllEvents() {
        return listAll();
    }
}
