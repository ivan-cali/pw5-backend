package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class TopicRepository implements PanacheMongoRepository<Topic> {

    private static final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]+$");

    private String validateAndSanitizeInput(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidInputException("Input cannot be null or empty.");
        }
        if (!SAFE_INPUT_PATTERN.matcher(input).matches()) {
            throw new InvalidInputException("Input contains invalid characters.");
        }
        return input.trim();
    }

    public Topic findByName(String name) {
        String sanitized = validateAndSanitizeInput(name);
        return find("name", sanitized.toLowerCase()).firstResult();
    }

    public Topic findTopicById(ObjectId topicId) {
        if (topicId == null) {
            throw new InvalidInputException("Topic ID cannot be null.");
        }
        return findById(topicId);
    }

    public boolean isAlreadyAFavourite(List<Topic> favouriteTopics, ObjectId topicId) {
        if (favouriteTopics == null || topicId == null) {
            throw new InvalidInputException("Favorite topics list and topic ID cannot be null.");
        }
        return favouriteTopics.stream().anyMatch(topic -> topic.getId().equals(topicId));
    }

    public List<Topic> getAll() {
        return listAll();
    }
}
