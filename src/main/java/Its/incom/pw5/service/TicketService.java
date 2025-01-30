package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

@ApplicationScoped
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    public TicketService(TicketRepository ticketRepository, EventRepository eventRepository, UserService userService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userService = userService;
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
    @Transactional
    public void deleteTicket(ObjectId ticketId, String userId) {
        // Retrieve the existing ticket using the ticketId
        Ticket existingTicket = ticketRepository.findById(ticketId);
        if (existingTicket == null) {
            throw new WebApplicationException("Ticket not found.", Response.Status.NOT_FOUND);
        }

        // Retrieve the user details
        User user;
        try {
            ObjectId objUserId = new ObjectId(userId);
            user = userService.getUserById(String.valueOf(objUserId));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid user ID format.", Response.Status.BAD_REQUEST);
        }

        if (user == null) {
            throw new WebApplicationException("User not found.", Response.Status.UNAUTHORIZED);
        }

        // Verify that the user has the ADMIN or HOST role
        if (!user.getRole().equals(Role.ADMIN)) {
            throw new WebApplicationException("User is not an Admin .", Response.Status.UNAUTHORIZED);
        }

        // Retrieve the related event
        Event relatedEvent = eventRepository.findById(existingTicket.getEventId());
        if (relatedEvent == null) {
            throw new WebApplicationException("Related event not found.", Response.Status.NOT_FOUND);
        }

        // Check if the event is archived
        if (relatedEvent.getStatus() != EventStatus.ARCHIVED) {
            throw new WebApplicationException("Cannot delete ticket. Related event is not archived.", Response.Status.BAD_REQUEST);
        }

        // Proceed to delete the ticket
        ticketRepository.deleteTicket(existingTicket);
    }
}


