package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.TicketStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.UUID;

@MongoEntity(collection = "ticket")
public class Ticket {
    private ObjectId id;
    private User user;
    private final String ticketCode;
    private Event event;
    private TicketStatus status;

    // No-argument constructor
    public Ticket() {
        this.ticketCode = UUID.randomUUID().toString(); // Generate UUID upon creation
    }

    // Parameterized constructor
    public Ticket(User user, Event event, TicketStatus status) {
        this.user = user;
        this.event = event;
        this.ticketCode = UUID.randomUUID().toString();
        this.status = status;
    }

    // Getters and Setters

    public String getTicketCode() {
        return ticketCode;
    }

    // Removed setter for ticketCode to prevent external modification

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ticket ticket = (Ticket) o;
        return Objects.equals(id, ticket.id) &&
                Objects.equals(user, ticket.user) &&
                Objects.equals(event, ticket.event) &&
                Objects.equals(ticketCode, ticket.ticketCode) &&
                status == ticket.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, event, ticketCode, status);
    }
}
