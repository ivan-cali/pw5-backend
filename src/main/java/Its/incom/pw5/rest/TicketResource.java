package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.rest.model.ConfirmTicketRequest;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.TicketService;
import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.rest.model.ConfirmTicketResponse;
import Its.incom.pw5.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.Map;

@ApplicationScoped
@Path("/ticket")
public class TicketResource {
    private final TicketService ticketService;
    private final SessionService sessionService;
    private final UserService userService;

    @Inject
    public TicketResource(TicketService ticketService, SessionService sessionService, UserService userService) {
        this.ticketService = ticketService;
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @POST
    @Path("/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmTicket(
            @CookieParam("SESSION_ID") String sessionId,
            @Valid ConfirmTicketRequest request) {

        if (sessionId == null || sessionId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.")
                    .build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.")
                    .build();
        }

        // Retrieve User
        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.")
                    .build();
        }

        // Confirm the ticket using user's ObjectId as hex string
        Ticket confirmedTicket = ticketService.confirmTicket(
                request.getTicketCode(),
                user.getId().toHexString()
        );

        return Response.ok(new ConfirmTicketResponse(confirmedTicket)).build();
    }
    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTicket(
            @CookieParam("SESSION_ID") String sessionId,
            Map<String, String> requestBody) {

        if (sessionId == null || sessionId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.")
                    .build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.")
                    .build();
        }

        // Retrieve User
        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.")
                    .build();
        }

        // Extract ticketId from request body
        String ticketIdStr = requestBody.get("ticketId");
        if (ticketIdStr == null || ticketIdStr.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ticketId is required in the request body.")
                    .build();
        }

        ObjectId objTicketId;
        try {
            objTicketId = new ObjectId(ticketIdStr);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid ticket ID format.")
                    .build();
        }

        try {
            // Attempt to delete the ticket
            ticketService.deleteTicket(objTicketId, user.getId().toHexString());
            return Response.ok("Ticket deleted successfully.").build();
        } catch (WebApplicationException e) {
            // Pass through the exception's status and message
            return Response.status(e.getResponse().getStatus())
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred while deleting the ticket.")
                    .build();
        }
    }
}
