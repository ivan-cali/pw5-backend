package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.UUID;

@ApplicationScoped
public class TicketRepository implements PanacheMongoRepository<Ticket> {

    public void addTicket(Ticket ticket) {
        persist(ticket);
    }

    public void updateTicket(Ticket assignedTicket) {
        // Find the ticket in the database by its ID
        Ticket existingTicket = findByIdOptional(assignedTicket.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + assignedTicket.getId()));

        // Update fields
        if (assignedTicket.getUserId() != null) {
            existingTicket.setUserId(assignedTicket.getUserId());
        }
        if (assignedTicket.getStatus() != null) {
            existingTicket.setStatus(assignedTicket.getStatus());
        }

        // Persist the updated ticket
        update(existingTicket);
    }
    public void nullifyUserId(Ticket ticket) {
        Ticket existingTicket = findByIdOptional(ticket.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticket.getId()));

        // Set userId to null
        existingTicket.setUserId(null);

        update(existingTicket);
    }
    public boolean deleteEmptyTicket(Ticket ticket) {
        delete(ticket);
        return true;
    }
    public void confirmTicket(Ticket ticket) {
        Ticket existingTicket = findByIdOptional(ticket.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticket.getId()));

        existingTicket.setStatus(TicketStatus.valueOf("CONFIRMED"));

        update(existingTicket);
    }

    public Ticket findByTicketCode(String ticketCode) {
        return find("ticketCode", ticketCode).firstResult();
    }

    public void refreshTicketCode(Ticket existingTicket) {
            existingTicket.setTicketCode(UUID.randomUUID().toString());
            update(existingTicket);
        }


    public void deleteTicket(Ticket existingTicket) {
        delete(existingTicket);
    }
}
