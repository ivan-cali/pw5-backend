package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@MongoEntity(collection = "adminNotification")
public class AdminNotification {
    private ObjectId id;
    private String type = "HOST_REQUEST";
    private String message;
    private LocalDateTime timestamp;
    private NotificationStatus status;
    private String hostEmail;
    private ObjectId hostId;
    private String handledBy;

    public ObjectId getHostId() {
        return hostId;
    }

    public void setHostId(ObjectId hostId) {
        this.hostId = hostId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public void setHostEmail(String hostEmail) {
        this.hostEmail = hostEmail;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }
}
