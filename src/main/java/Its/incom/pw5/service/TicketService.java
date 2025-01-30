package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import Its.incom.pw5.persistence.repository.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }


    @Transactional
    public Ticket confirmTicket(String ticketCode, String userId) {
        // Retrieve the existing ticket using the ticket code
        Ticket existingTicket = ticketRepository.findByTicketCode(ticketCode);

        if (existingTicket == null) {
            throw new WebApplicationException("Invalid ticket code.", Response.Status.NOT_FOUND);
        }

        // Verify that the ticket belongs to the user
        if (!existingTicket.getUserId().toHexString().equals(userId)) {
            throw new WebApplicationException("Unauthorized to confirm this ticket.", Response.Status.FORBIDDEN);
        }

        if (existingTicket.getStatus() == TicketStatus.CONFIRMED) {
            throw new WebApplicationException("Ticket is already confirmed.", Response.Status.BAD_REQUEST);
        }

        // Update the status to CONFIRMED
        existingTicket.setStatus(TicketStatus.CONFIRMED);

        // Persist the updated ticket
        ticketRepository.update(existingTicket);

        return existingTicket;
    }
}

