package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.Map;

@GlobalLog
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
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Ticket not found."))
                    .build());
        }

        // Verify that the ticket belongs to the user
        if (!existingTicket.getUserId().toHexString().equals(userId)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Ticket does not belong to the user."))
                    .build());
        }

        if (existingTicket.getStatus() == TicketStatus.CONFIRMED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Ticket is already confirmed."))
                    .build());
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
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Ticket not found."))
                    .build());
        }

        // Retrieve the user details
        User user;
        try {
            ObjectId objUserId = new ObjectId(userId);
            user = userService.getUserById(String.valueOf(objUserId));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid user ID."))
                    .build());
        }

        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "User not found."))
                    .build());
        }

        // Retrieve the related event
        Event relatedEvent = eventRepository.findById(existingTicket.getEventId());
        if (relatedEvent == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Related event not found."))
                    .build());
        }

        // Check if the event is archived
        if (relatedEvent.getStatus() != EventStatus.ARCHIVED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Cannot delete ticket for an active event."))
                    .build());
        }

        // Proceed to delete the ticket
        ticketRepository.deleteTicket(existingTicket);
    }
}


