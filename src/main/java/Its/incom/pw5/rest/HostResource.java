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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Path("/host")
public class HostResource {
    private final HostService hostService;
    private final SessionService sessionService;
    private final UserService userService;
    private final EventService eventService;

    public HostResource(HostService hostService, SessionService sessionService, UserService userService, EventService eventService) {
        this.hostService = hostService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.eventService = eventService;
    }

    //get all hosts
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getAll() {
        try {
            List<Host> hosts = hostService.getAll();
            Map<String, Object> responseBody = Map.of(
                    "message", "Hosts retrieved successfully.",
                    "hosts", hosts
            );
            return Response.ok(responseBody).build();
        } catch (HostNotFoundException e) {
            Map<String, Object> responseBody = Map.of(
                    "message", e.getMessage()
            );
            return Response.status(Response.Status.NOT_FOUND).entity(responseBody).build();
        }
    }

    @PUT
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response changePsw(@CookieParam("SESSION_ID") String sessionId, PasswordEditRequest passwordEditRequest) {
        try {
            //find user session
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Session cookie not found."))
                        .build();
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Invalid session cookie."))
                        .build();
            }

            User user = userService.getUserById(session.getUserId());
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "User not found."))
                        .build();
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

            Map<String, Object> responseBody = Map.of(
                    "message", "Password updated",
                    "host", host
            );

            return Response.ok(responseBody).cookie(sessionCookie).build();
        } catch (HostNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        } catch (HostUpdateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
    }


    @PUT
    @Path("confirm-event/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response confirmEvent(@CookieParam("SESSION_ID") String sessionId, @PathParam("id") ObjectId eventId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Session cookie not found."))
                    .build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid session cookie."))
                    .build();
        }

        Host host = hostService.getHostById(session.getUserId());
        if (host == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Host not found."))
                    .build();
        }

        Event event = new Event();
        event.setId(eventId);
        event = eventService.getEventById(event);

        //event is created by this host
        if (!event.getHost().equals(host.getEmail())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Unauthorized access."))
                    .build();
        }

        //check event status is still PENDING
        if (!event.getStatus().equals(EventStatus.PENDING)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event status is not PENDING."))
                    .build();
        }

        //User in speakers list are speakers with status already confirmed
        if (event.getSpeakers() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Speakers list is empty."))
                    .build();
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

        Map<String, Object> responseBody = Map.of(
                "message", "Event confirmed successfully.",
                "event", event
        );

        return Response.ok(responseBody).build();
    }

    //Update host description (for the host profile)
    @PUT
    @Path("/update-description")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDescription(@CookieParam("SESSION_ID") String sessionId, Host host) {
        try {
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Session cookie not found."))
                        .build();
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Invalid session cookie."))
                        .build();
            }

            Host loggedHost = hostService.getHostById(session.getUserId());
            if (host == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Host not found."))
                        .build();
            }

            hostService.updateDescription(loggedHost, host.getDescription());

            Map<String, Object> responseBody = Map.of(
                    "message", "Description updated successfully."
            );

            return Response.ok(responseBody).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
    }
}
