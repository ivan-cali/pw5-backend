package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.service.EventService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
}
