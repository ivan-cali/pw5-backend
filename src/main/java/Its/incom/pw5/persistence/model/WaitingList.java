package Its.incom.pw5.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;

@MongoEntity(collection = "waitingList")
public class WaitingList {
    private ObjectId id;
    private ObjectId eventId;
    private List<String> WaitingUsers;

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
        return WaitingUsers;
    }

    public void setWaitingUsers(List<String> waitingUsers) {
        this.WaitingUsers = waitingUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WaitingList that = (WaitingList) o;
        return Objects.equals(id, that.id) && Objects.equals(eventId, that.eventId) && Objects.equals(WaitingUsers, that.WaitingUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventId, WaitingUsers);
    }
}
