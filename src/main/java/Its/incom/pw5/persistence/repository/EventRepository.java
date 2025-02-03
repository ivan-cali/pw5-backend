package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class EventRepository implements PanacheMongoRepository<Event> {

    // Regex to validate strings for topics and email fields
    private static final Pattern SAFE_TOPIC_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-\\s]+$");
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Validates a string input for topics
    private String validateAndSanitizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new InvalidInputException("Topic cannot be null or empty.");
        }
        if (!SAFE_TOPIC_PATTERN.matcher(topic).matches()) {
            throw new InvalidInputException("Invalid topic format.");
        }
        return topic.trim();
    }

    // Validates and sanitizes an email
    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("Email cannot be null or empty.");
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException("Invalid email format.");
        }
        return email.trim();
    }

    public void addEvent(Event event) {
        persist(event);
    }

    public void updateEvent(Event existingEvent) {
        update(existingEvent);
    }

    // Fetches events by validated topic list
    public List<Event> getEventsByTopic(List<String> topics) {
        topics.forEach(this::validateAndSanitizeTopic);
        return list("topics", topics);
    }

    // Fetches events by date (no input sanitization needed for LocalDateTime)
    public List<Event> getEventsByDate(LocalDateTime date) {
        if (date == null) {
            throw new InvalidInputException("Date cannot be null.");
        }
        return list("date", date);
    }

    // Fetches events by validated speaker email
    public List<Event> getEventsBySpeakerMail(String speakerMail) {
        String sanitizedEmail = validateAndSanitizeEmail(speakerMail);
        return list("speakers.email", sanitizedEmail);
    }

    public List<Event> getAllEvents() {
        return listAll();
    }

    public void deleteEvent(Event event) {
        delete(event);
    }
}
