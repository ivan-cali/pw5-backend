package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.EventSubscription;
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
    private String description;
    private String host;
    private String title;
    private EventStatus status;
    private EventSubscription EventSubscription;
    private List<User> pendingSpeakerRequests;
    private int maxParticipants;
    private int registeredParticipants;
    private List<ObjectId> ticketIds;


    public EventSubscription getEventSubscription() {
        return EventSubscription;
    }

    public void setEventSubscription(EventSubscription eventSubscription) {
        EventSubscription = eventSubscription;
    }

    public List<User> getPendingSpeakerRequests() {
        return pendingSpeakerRequests;
    }

    public void setPendingSpeakerRequests(List<User> pendingSpeakerRequests) {
        this.pendingSpeakerRequests = pendingSpeakerRequests;
    }
    public List<ObjectId> getTicketIds() {
        return ticketIds;
    }

    public void setTicketIds(List<ObjectId> ticketIds) {
        this.ticketIds = ticketIds;
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getRegisteredParticipants() {
        return registeredParticipants;
    }

    public void setRegisteredParticipants(int registeredParticipants) {
        this.registeredParticipants = registeredParticipants;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return maxParticipants == event.maxParticipants && registeredParticipants == event.registeredParticipants && Objects.equals(id, event.id) && Objects.equals(startDate, event.startDate) && Objects.equals(endDate, event.endDate) && Objects.equals(place, event.place) && Objects.equals(speakers, event.speakers) && Objects.equals(topics, event.topics) && Objects.equals(description, event.description) && Objects.equals(host, event.host) && Objects.equals(title, event.title) && status == event.status && EventSubscription == event.EventSubscription && Objects.equals(pendingSpeakerRequests, event.pendingSpeakerRequests) && Objects.equals(ticketIds, event.ticketIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, place, speakers, topics, description, host, title, status, EventSubscription, pendingSpeakerRequests, maxParticipants, registeredParticipants, ticketIds);
    }
}
