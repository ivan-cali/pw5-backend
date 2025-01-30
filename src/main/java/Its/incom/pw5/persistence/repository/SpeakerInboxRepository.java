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

    public boolean speakerWithConfirmedStatus(List<SpeakerInbox> speakers) {
        return speakers.stream().anyMatch(s -> s.getStatus().equals(SpeakerInboxStatus.CONFIRMED));
    }

    public void deleteRequest(ObjectId inboxId) {
        deleteById(inboxId);
    }

    public List<SpeakerInbox> getRequestsByEventId(ObjectId id) {
        return find("eventId", id).list();
    }
}
