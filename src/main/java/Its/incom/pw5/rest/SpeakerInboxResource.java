package Its.incom.pw5.rest;

import Its.incom.pw5.service.SpeakerInboxService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

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
            return Response.ok(speakerInboxService.confirmRequest(inboxId)).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(ex.getMessage()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred.").build();
        }
    }

    @PUT
    @Path("/{inboxId}/reject")
    public Response rejectRequest(@PathParam("inboxId") ObjectId inboxId) {
        try {
            return Response.ok(speakerInboxService.rejectRequest(inboxId)).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(ex.getMessage()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred.").build();
        }
    }
}