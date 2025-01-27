package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Event;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventRepository implements PanacheMongoRepository<Event> {

    public void addEvent(Event event) {
        persist(event);
    }
}
