package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.WaitingList;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class WaitingListRepository implements PanacheMongoRepository<WaitingList> {

    // Validate and sanitize ObjectId
    private ObjectId validateAndSanitizeObjectId(ObjectId id) {
        if (id == null) {
            throw new InvalidInputException("ID cannot be null.");
        }
        return id;
    }

    public void addWaitingList(WaitingList waitingList) {
        persist(waitingList);
    }

    public void addUserToWaitingList(WaitingList waitingList) {
        update(waitingList);
    }

    public WaitingList getWaitingListByEventId(ObjectId id) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(id);
        return find("eventId", sanitizedId).firstResult();
    }

    public void updateWaitingList(WaitingList waitingList) {
        update(waitingList);
    }

    public void deleteWaitingList(WaitingList waitingList) {
        delete(waitingList);
    }
}
