package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.SpeakerInbox;
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
}
