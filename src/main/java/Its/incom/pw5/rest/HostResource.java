package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.rest.model.PasswordEditRequest;
import Its.incom.pw5.service.*;
import Its.incom.pw5.service.exception.HostNotFoundException;
import Its.incom.pw5.service.exception.HostUpdateException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Path("/host")
public class HostResource {
    //class injection
    @Inject
    HostService hostService;
    @Inject
    MailService mailService;
    @Inject
    SessionService sessionService;
    @Inject
    UserService userService;
    @Inject
    EventService eventService;

    //get all hosts
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            return Response.ok(hostService.getAll()).build();
        } catch (HostNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePsw(@CookieParam("SESSION_ID") String sessionId, PasswordEditRequest passwordEditRequest) {
        try {
            //find user session
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
            }

            User user = userService.getUserById(session.getUserId());
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("User not found.").build();
            }

            Host host = hostService.getHostByUserCreatorEmail(user.getEmail());

            if (host == null) {
                throw new HostNotFoundException("Host not found");
            }

            //change generated password with a new password
            hostService.changeHostPsw(host, passwordEditRequest);

            //create a session for the new host
            Session hostSession = sessionService.createOrReuseSession(host.getId().toString());
            String sessionCookieValue = hostSession.getCookieValue();

            NewCookie sessionCookie = new NewCookie(
                    "SESSION_ID",           // Cookie name
                    sessionCookieValue,     // Cookie value
                    "/",                    // Path
                    null,                   // Domain (null uses request domain)
                    null,                   // Comment
                    (int) java.time.Duration.between(LocalDateTime.now(), session.getExpiresIn()).getSeconds(), // Max age in seconds
                    false                   // Secure flag (true if using HTTPS)
            );

            return Response.ok()
                    .entity("Password updated")
                    .cookie(sessionCookie)
                    .build();
        } catch (HostNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (HostUpdateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @PUT
    @Path("confirm-event/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmEvent(@CookieParam("SESSION_ID") String sessionId, @PathParam("id") ObjectId eventId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        Host host = hostService.getHostById(session.getUserId());
        if (host == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Host not found.").build();
        }

        Event event = new Event();
        event.setId(eventId);
        event = eventService.getEventById(event);

        //event is created by this host
        if (!event.getHost().equals(host.getEmail())) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(host.getName() + " isn't the creator of " + event.getTitle()).build();
        }

        //check event status is still PENDING
        if (!event.getStatus().equals(EventStatus.PENDING)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(event.getTitle() + " already confirmed.").build();
        }

        //User in speakers list are speakers with status already confirmed
        if (event.getSpeakers() == null){
            return Response.status(Response.Status.BAD_REQUEST).entity(event.getTitle() + " doesn't have confirmed speakers.").build();
        }

        //update event status and host programmed events
        event.setStatus(EventStatus.CONFIRMED);
        eventService.updateEventStatus(event);

        List<Event> hostProgrammedEvents = host.getProgrammedEvents();
        if (hostProgrammedEvents == null) {
            hostProgrammedEvents = new ArrayList<>();
        }
        hostProgrammedEvents.add(event);
        host.setProgrammedEvents(hostProgrammedEvents);
        hostService.updateEvents(host);

        return Response.ok(event).build();
    }


    //update an exsisting host
//    @PATCH
//    @Path("/{id}/update")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response update(@CookieParam("SESSION_ID") String sessionId, @PathParam("id") ObjectId id, Map<String, Object> updates){
//        try{
//            //controllo sessione
//            Host host = hostService.update(id, updates);
//            return Response.ok().entity(host).build();
//        } catch (HostUpdateException e){
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
//        }
//    }


    //delete an host
    /*@DELETE
    @Path("{Id}/delete")
    public Response delete(@CookieParam("SESSION_ID") Cookie sessionCookie){
        try {

        } catch ()
    }*/


}
