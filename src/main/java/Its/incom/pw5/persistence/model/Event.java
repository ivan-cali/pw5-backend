package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.EventStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@MongoEntity(collection = "event")
public class Event {
    private ObjectId id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String place;
    private List<User> speakers;
    private List<String> topics;
    private List<Host> hosts;
    private String title;
    private EventStatus status;
    private List<User> pendingSpeakerRequests;
    private int maxPartecipants;
    private int registerdPartecipants;

    public List<User> getPendingSpeakerRequests() {
        return pendingSpeakerRequests;
    }

    public void setPendingSpeakerRequests(List<User> pendingSpeakerRequests) {
        this.pendingSpeakerRequests = pendingSpeakerRequests;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return maxPartecipants == event.maxPartecipants && registerdPartecipants == event.registerdPartecipants && Objects.equals(id, event.id) && Objects.equals(startDate, event.startDate) && Objects.equals(endDate, event.endDate) && Objects.equals(place, event.place) && Objects.equals(speakers, event.speakers) && Objects.equals(topics, event.topics) && Objects.equals(hosts, event.hosts) && Objects.equals(title, event.title) && status == event.status && Objects.equals(pendingSpeakerRequests, event.pendingSpeakerRequests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, place, speakers, topics, hosts, title, status, pendingSpeakerRequests, maxPartecipants, registerdPartecipants);
    }
}
