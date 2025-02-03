package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class TicketRepository implements PanacheMongoRepository<Ticket> {

    private static final Pattern SAFE_TICKET_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    // Validate and sanitize ObjectId
    private ObjectId validateAndSanitizeObjectId(ObjectId id) {
        if (id == null) {
            throw new InvalidInputException("ID cannot be null.");
        }
        return id;
    }

    // Validate and sanitize ticket code
    private String validateAndSanitizeTicketCode(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw new InvalidInputException("Ticket code cannot be null or empty.");
        }
        if (!SAFE_TICKET_CODE_PATTERN.matcher(ticketCode).matches()) {
            throw new InvalidInputException("Invalid ticket code format.");
        }
        return ticketCode.trim();
    }

    // Validate TicketStatus
    private TicketStatus validateAndSanitizeStatus(TicketStatus status) {
        if (status == null) {
            throw new InvalidInputException("Ticket status cannot be null.");
        }
        return status;
    }

    public void addTicket(Ticket ticket) {
        persist(ticket);
    }

    public void updateTicket(Ticket assignedTicket) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(assignedTicket.getId());

        Ticket existingTicket = findByIdOptional(sanitizedId)
                .orElseThrow(() -> new InvalidInputException("Ticket not found with ID: " + sanitizedId));

        if (assignedTicket.getUserId() != null) {
            existingTicket.setUserId(assignedTicket.getUserId());
        }
        if (assignedTicket.getStatus() != null) {
            existingTicket.setStatus(validateAndSanitizeStatus(assignedTicket.getStatus()));
        }

        update(existingTicket);
    }

    public void nullifyUserId(Ticket ticket) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(ticket.getId());

        Ticket existingTicket = findByIdOptional(sanitizedId)
                .orElseThrow(() -> new InvalidInputException("Ticket not found with ID: " + sanitizedId));

        existingTicket.setUserId(null);
        update(existingTicket);
    }

    public boolean deleteEmptyTicket(Ticket ticket) {
        delete(ticket);
        return true;
    }

    public void confirmTicket(Ticket ticket) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(ticket.getId());

        Ticket existingTicket = findByIdOptional(sanitizedId)
                .orElseThrow(() -> new InvalidInputException("Ticket not found with ID: " + sanitizedId));

        existingTicket.setStatus(TicketStatus.CONFIRMED);
        update(existingTicket);
    }

    public Ticket findByTicketCode(String ticketCode) {
        String sanitizedCode = validateAndSanitizeTicketCode(ticketCode);
        return find("ticketCode", sanitizedCode).firstResult();
    }

    public void refreshTicketCode(Ticket existingTicket) {
        existingTicket.setTicketCode(UUID.randomUUID().toString());
        update(existingTicket);
    }

    public void deleteTicket(Ticket existingTicket) {
        delete(existingTicket);
    }
}
