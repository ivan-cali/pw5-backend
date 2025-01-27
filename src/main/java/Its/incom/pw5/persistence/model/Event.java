package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.EventStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@MongoEntity(collection = "event")
public class Event {
    private ObjectId id;
    private LocalDateTime date;
    private String place;
    private List<User> speakers;
    private List<String> topics;
    private List<Host> hosts;
    private String title;
    private EventStatus status;
    private int maxPartecipants;
    private int registerdPartecipants;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public List<User> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<User> speakers) {
        this.speakers = speakers;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public int getMaxPartecipants() {
        return maxPartecipants;
    }

    public void setMaxPartecipants(int maxPartecipants) {
        this.maxPartecipants = maxPartecipants;
    }

    public int getRegisterdPartecipants() {
        return registerdPartecipants;
    }

    public void setRegisterdPartecipants(int registerdPartecipants) {
        this.registerdPartecipants = registerdPartecipants;
    }
}
