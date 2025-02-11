package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.Ticket;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.rest.model.ConfirmTicketRequest;
import Its.incom.pw5.rest.model.ConfirmTicketResponse;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.TicketService;
import Its.incom.pw5.service.UserService;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

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
    public Response confirmTicket(@Valid ConfirmTicketRequest request) {
        // Confirm ticket based only on the ticket code
        Ticket confirmedTicket = ticketService.confirmTicket(request.getTicketCode(), null);

        if (confirmedTicket == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Invalid ticket code."))
                    .build();
        }

        return Response.ok(Map.of(
                "message", "Ticket confirmed successfully.",
                "ticket", new ConfirmTicketResponse(confirmedTicket)
        )).build();
    }



    @DELETE
    @Path("/delete")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTicket(@CookieParam("SESSION_ID") String sessionId, Map<String, String> requestBody) {

        if (sessionId == null || sessionId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Session ID is required."))
                    .build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid session ID."))
                    .build();
        }

        // Retrieve User
        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }

        // Extract ticketId from request body
        String ticketIdStr = requestBody.get("ticketId");
        if (ticketIdStr == null || ticketIdStr.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "ticketId is required in the request body."))
                    .build();
        }

        ObjectId objTicketId;
        try {
            objTicketId = new ObjectId(ticketIdStr);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Invalid ticket ID format."))
                    .build();
        }

        try {
            // Attempt to delete the ticket
            ticketService.deleteTicket(objTicketId, user.getId().toHexString());

            Map<String, Object> responseBody = Map.of(
                    "message", "Ticket deleted successfully."
            );

            return Response.ok(responseBody)
                    .build();
        } catch (WebApplicationException e) {
            return Response.status(e.getResponse().getStatus())
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "An unexpected error occurred while deleting the ticket."))
                    .build();
        }
    }
}
