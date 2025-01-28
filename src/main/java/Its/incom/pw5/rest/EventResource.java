package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.service.EventService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    EventService eventService;

    @POST
    public Response createEvent(Event event) {
        if (event == null) {
            // Return a bad request if no event data was provided
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build();
        }

        Event createdEvent = eventService.createEvent(event);
        return Response.status(Response.Status.CREATED).entity(createdEvent).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateEvent(@PathParam("id") ObjectId id, Event updatedEvent) {
        try {
            // Ensure the event is updated only if the date is at least 2 weeks away
            Event event = eventService.updateEvent(id, updatedEvent);
            return Response.ok(event).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(ex.getMessage()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred.").build();
        }
    }
}
