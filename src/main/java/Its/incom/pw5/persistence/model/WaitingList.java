package Its.incom.pw5.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;

@MongoEntity(collection = "waitingList")
public class WaitingList {
    private ObjectId id;
    private ObjectId eventId;
    private List<String> waitingUsers;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getEventId() {
        return eventId;
    }

    public void setEventId(ObjectId eventId) {
        this.eventId = eventId;
    }

    public List<String> getWaitingUsers() {
        return waitingUsers;
    }

    public void setWaitingUsers(List<String> waitingUsers) {
        this.waitingUsers = waitingUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WaitingList that = (WaitingList) o;
        return Objects.equals(id, that.id) && Objects.equals(eventId, that.eventId) && Objects.equals(waitingUsers, that.waitingUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventId, waitingUsers);
    }
}
