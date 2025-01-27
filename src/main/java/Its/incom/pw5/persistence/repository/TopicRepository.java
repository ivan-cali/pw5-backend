package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Topic;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {

    // Find a topic by name (stored in lower case).
    public Topic findByName(String name) {
        // We store all names in lower case, so convert search to lower case
        return find("name", name.toLowerCase()).firstResult();
    }
}
