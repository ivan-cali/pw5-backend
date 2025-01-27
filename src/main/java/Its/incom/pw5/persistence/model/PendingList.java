package Its.incom.pw5.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.ArrayList;

@MongoEntity(collection = "pendingList")
public class PendingList {
    private ObjectId id;
    private ObjectId eventId;
    private ArrayList<String> pendingUsers;

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

    public ArrayList<String> getPendingUsers() {
        return pendingUsers;
    }

    public void setPendingUsers(ArrayList<String> pendingUsers) {
        this.pendingUsers = pendingUsers;
    }
}
