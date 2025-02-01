package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.service.SpeakerInboxService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@Path("/speaker-inbox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SpeakerInboxResource {

    @Inject
    SpeakerInboxService speakerInboxService;

    @PUT
    @Path("/{inboxId}/confirm")
    public Response confirmRequest(@PathParam("inboxId") ObjectId inboxId) {
        try {
            SpeakerInbox confirmedRequest = speakerInboxService.confirmRequest(inboxId);

            Map<String, Object> responseBody = Map.of(
                    "message", "Request confirmed successfully.",
                    "request", confirmedRequest
            );

            return Response.ok(responseBody).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(Map.of("message", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "An unexpected error occurred."))
                    .build();
        }
    }

    @PUT
    @Path("/{inboxId}/reject")
    public Response rejectRequest(@PathParam("inboxId") ObjectId inboxId) {
        try {
            SpeakerInbox rejectedRequest = speakerInboxService.rejectRequest(inboxId);

            Map<String, Object> responseBody = Map.of(
                    "message", "Request rejected successfully.",
                    "request", rejectedRequest
            );

            return Response.ok(responseBody).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(Map.of("message", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "An unexpected error occurred."))
                    .build();
        }
    }

    @GET
    @Path("/my-requests")
    public Response getMyRequests(@CookieParam("SESSION_ID") String sessionCookie, @QueryParam("status") SpeakerInboxStatus requestStatus) {
        try {
            if (sessionCookie == null || sessionCookie.isBlank()) {
                throw new WebApplicationException("Session cookie is required", 401);
            }

            List<SpeakerInbox> userRequests = speakerInboxService.getRequestsForUser(sessionCookie, requestStatus);

            if (userRequests == null || userRequests.isEmpty()) {
                return Response.ok(Map.of("message", "You have 0 requests.")).build();
            }

            Map<String, Object> responseBody = Map.of(
                    "message", "Requests retrieved successfully.",
                    "requests", userRequests
            );

            return Response.ok(responseBody).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(Map.of("message", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "An unexpected error occurred."))
                    .build();
        }
    }
}