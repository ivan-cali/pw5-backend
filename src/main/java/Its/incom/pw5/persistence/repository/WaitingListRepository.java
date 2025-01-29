package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.WaitingList;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class WaitingListRepository implements PanacheMongoRepository<WaitingList> {

    public void addWaitingList(WaitingList waitingList) {
        persist(waitingList);
    }

    public void addUserToWaitingList(WaitingList waitingList) {
        update(waitingList);
    }

    public WaitingList getWaitingListByEventId(ObjectId id) {
        return find("eventId", id).firstResult();
    }
}
