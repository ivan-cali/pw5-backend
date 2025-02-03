package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.service.exception.InvalidInputException;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class SpeakerInboxRepository implements PanacheMongoRepository<SpeakerInbox> {

    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Validate and sanitize ObjectId
    private ObjectId validateAndSanitizeObjectId(ObjectId id) {
        if (id == null) {
            throw new InvalidInputException("ID cannot be null.");
        }
        return id;
    }

    // Validate and sanitize email
    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("Email cannot be null or empty.");
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException("Invalid email format.");
        }
        return email.trim();
    }

    // Validate SpeakerInboxStatus input
    private SpeakerInboxStatus validateAndSanitizeStatus(SpeakerInboxStatus status) {
        if (status == null) {
            throw new InvalidInputException("Speaker inbox status cannot be null.");
        }
        return status;
    }

    public void addSpeakerInbox(SpeakerInbox inbox) {
        persist(inbox);
    }

    public void update(SpeakerInbox inbox) {
        persistOrUpdate(inbox);
    }

    public SpeakerInbox findById(ObjectId id) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(id);
        return findByIdOptional(sanitizedId).orElse(null);
    }

    public List<SpeakerInbox> findBySpeakerEmail(String email) {
        String sanitizedEmail = validateAndSanitizeEmail(email);
        return find("speakerEmail", sanitizedEmail).list();
    }

    public List<SpeakerInbox> getRequestsByStatus(SpeakerInboxStatus status) {
        SpeakerInboxStatus sanitizedStatus = validateAndSanitizeStatus(status);
        return find("status", sanitizedStatus).list();
    }

    public void deleteRequest(ObjectId inboxId) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(inboxId);
        deleteById(sanitizedId);
    }

    public List<SpeakerInbox> getRequestsByEventId(ObjectId id) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(id);
        return find("eventId", sanitizedId).list();
    }

    public boolean existsBySpeakerEmailAndEventId(String speakerEmail, ObjectId eventId) {
        String sanitizedEmail = validateAndSanitizeEmail(speakerEmail);
        ObjectId sanitizedEventId = validateAndSanitizeObjectId(eventId);
        return find("speakerEmail = ?1 and eventId = ?2", sanitizedEmail, sanitizedEventId)
                .firstResultOptional().isPresent();
    }

    public List<SpeakerInbox> findConfirmedRequestsByEmail(String speakerEmail) {
        String sanitizedEmail = validateAndSanitizeEmail(speakerEmail);
        return find("speakerEmail = ?1 and status = ?2", sanitizedEmail, SpeakerInboxStatus.CONFIRMED).list();
    }
}
