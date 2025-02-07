package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.service.SpeakerInboxService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.util.List;
import java.util.Map;

@Path("/speaker-inbox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Counted(name = "api_calls_total", description = "Total number of API calls")
@Timed(name = "api_call_duration", description = "Time taken to process API calls")
public class SpeakerInboxResource {
    private final SpeakerInboxService speakerInboxService;

    public SpeakerInboxResource(SpeakerInboxService speakerInboxService) {
        this.speakerInboxService = speakerInboxService;
    }


    @PUT
    @Path("/{inboxId}/confirm")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getMyRequests(@CookieParam("SESSION_ID") String sessionCookie, @QueryParam("status") SpeakerInboxStatus requestStatus) {
        try {
            if (sessionCookie == null || sessionCookie.isBlank()) {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "You must be logged in to access this resource."))
                        .build());
            }

            List<SpeakerInbox> userRequests = speakerInboxService.getRequestsForUser(sessionCookie, requestStatus);

            if (userRequests == null || userRequests.isEmpty()) {
                Map<String, Object> responseBody = Map.of(
                        "message", "You have 0 requests.",
                        "requests", List.of()
                );

                return Response.ok(responseBody)
                        .build();
            }

            Map<String, Object> responseBody = Map.of(
                    "message", "Requests retrieved successfully.",
                    "requests", userRequests
            );

            return Response.ok(responseBody)
                    .build();
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
    @Path("/{speakerId}/requests")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getSpeakerConfirmedRequests(@PathParam("speakerId") ObjectId speakerId) {
        try {
            if (speakerId == null) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Speaker ID cannot be null."))
                        .build());
            }

            List<SpeakerInbox> speakerRequests = speakerInboxService.getConfirmedRequestsForSpeaker(speakerId);

            if (speakerRequests == null || speakerRequests.isEmpty()) {
                Map<String, Object> responseBody = Map.of(
                        "message", "Speaker has 0 requests.",
                        "requests", List.of()
                );

                return Response.ok(responseBody)
                        .build();
            }

            Map<String, Object> responseBody = Map.of(
                    "message", "Requests retrieved successfully.",
                    "requests", speakerRequests
            );

            return Response.ok(responseBody)
                    .build();
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