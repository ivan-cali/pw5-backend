package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.TicketStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.UUID;

@MongoEntity(collection = "ticket")
public class Ticket {
    private ObjectId id;
    private ObjectId userId;
    private ObjectId eventId;
    private String ticketCode;
    private TicketStatus status;
    private String qrCodeUrl;

    // No-argument constructor
    public Ticket() {
        this.ticketCode = UUID.randomUUID().toString(); // Generate UUID upon creation
    }

    // Updated parameterized constructor
    public Ticket(ObjectId userId, ObjectId eventId, TicketStatus status) {
        this.userId = userId;
        this.eventId = eventId;
        this.ticketCode = UUID.randomUUID().toString();
        this.status = status;
    }

    // Getters and Setters

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ObjectId getEventId() {
        return eventId;
    }

    public void setEventId(ObjectId eventId) {
        this.eventId = eventId;
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
                Objects.equals(userId, ticket.userId) &&
                Objects.equals(eventId, ticket.eventId) &&
                Objects.equals(ticketCode, ticket.ticketCode) &&
                status == ticket.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, eventId, ticketCode, status);
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
}
