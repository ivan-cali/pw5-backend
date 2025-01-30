package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class SpeakerInboxRepository implements PanacheMongoRepository<SpeakerInbox> {

    public void addSpeakerInbox(SpeakerInbox inbox) {
        persist(inbox);
    }

    public void update(SpeakerInbox inbox) {
        persistOrUpdate(inbox);
    }

    public SpeakerInbox findById(ObjectId id) {
        return findByIdOptional(id).orElse(null);
    }

    public List<SpeakerInbox> findBySpeakerEmail(String email) {
        return find("speakerEmail", email).list();
    }

    public List<SpeakerInbox> getRequestsByStatus(SpeakerInboxStatus status) {
        return find("status", status).list();
    }

    public void deleteRequest(ObjectId inboxId) {
        deleteById(inboxId);
    }

    public List<SpeakerInbox> getRequestsByEventId(ObjectId id) {
        return find("eventId", id).list();
    }

    public boolean existsBySpeakerEmailAndEventId(String speakerEmail, ObjectId eventId) {
        return find("speakerEmail = ?1 and eventId = ?2", speakerEmail, eventId)
                .firstResultOptional().isPresent();
    }

    public List<SpeakerInbox> findConfirmedRequestsByEmail(String speakerEmail) {
        // Query the database for all speaker inbox requests with status CONFIRMED for the given email
        return find("speakerEmail = ?1 and status = ?2", speakerEmail, SpeakerInboxStatus.CONFIRMED).list();
    }
}
