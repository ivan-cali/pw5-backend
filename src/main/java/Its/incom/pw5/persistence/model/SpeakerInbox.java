package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity(collection = "speakerEventRequest")
public class SpeakerInbox {
    private ObjectId id;
    private String speakerEmail;
    private ObjectId eventId;
    private SpeakerInboxStatus status;

    // No-argument constructor
    public SpeakerInbox() {
    }

    public SpeakerInbox(ObjectId id, String speakerEmail, ObjectId eventId, SpeakerInboxStatus status) {
        this.id = id;
        this.speakerEmail = speakerEmail;
        this.eventId = eventId;
        this.status = status;
    }

    public String getSpeakerEmail() {
        return speakerEmail;
    }

    public void setSpeakerEmail(String speakerEmail) {
        this.speakerEmail = speakerEmail;
    }

    public ObjectId getEventId() {
        return eventId;
    }

    public void setEventId(ObjectId eventId) {
        this.eventId = eventId;
    }

    public SpeakerInboxStatus getStatus() {
        return status;
    }

    public void setStatus(SpeakerInboxStatus status) {
        this.status = status;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}