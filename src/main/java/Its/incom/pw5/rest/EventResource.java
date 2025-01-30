package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.service.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Path("/event")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {
    private final EventService eventService;
    private final SessionService sessionService;
    private final UserService userService;
    private final MailService mailService;
    private final HostService hostService;

    public EventResource(EventService eventService, SessionService sessionService, UserService userService, MailService mailService, HostService hostService) {
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.mailService = mailService;
        this.hostService = hostService;
    }

    @POST
    public Response createEvent(@CookieParam("SESSION_ID") String sessionId, Event event) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }


        String email;

        // Check if session provides a User with admin role
        User user = userService.getUserById(session.getUserId());
        if (user != null && user.getRole() == Role.ADMIN) {
            email = user.getEmail();
        } else {
            // Check if sessions provides a Host
            Host host = hostService.getHostById(session.getUserId());
            if (host != null) {
                email = host.getEmail();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("User is neither an Admin nor a Host.").build();
            }
        }


        if (event == null) {
            // Return a bad request if no event data was provided
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build();
        }

        Event createdEvent = eventService.createEvent(event, email);
        return Response.status(Response.Status.CREATED).entity(createdEvent).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEvent(@PathParam("id") ObjectId id, Event updatedEvent, @QueryParam("speakerEmail") String speakerEmail) {
        try {
            // Ensure the event is updated only if the date is at least 2 weeks away
            Event event = eventService.updateEvent(id, updatedEvent, speakerEmail);
            return Response.ok(event).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(ex.getMessage()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred.").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEvent(@PathParam("id") ObjectId id, @CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.").build();
        }

        Host host = hostService.getHostById(session.getUserId());
        if (host == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not an admin or host.").build();
        }

        User user = userService.getUserByEmail(host.getCreatedBy());
        if (user == null || user.getRole() != Role.ADMIN) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not an admin or host.").build();
        }

        eventService.deleteEvent(id, host);
        return Response.ok().entity("Event deleted successfully.").build();
    }

    @PUT
    @Path("/book")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bookEvent(@CookieParam("SESSION_ID") String sessionId, Event eventId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.").build();
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not verified.").build();
        }

        eventService.checkAndBookEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingConfirmationMail(user.getEmail(), event);

        return Response.ok().entity("Event booked successfully. An email confirmation has been sent.").build();
    }

    @PUT
    @Path("/revoke")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response revokeEvent(@CookieParam("SESSION_ID") String sessionId, Event eventId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.").build();
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not verified.").build();
        }

        eventService.checkAndRevokeEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingRevocationMail(user.getEmail(), event);

        return Response.ok().entity("Event revoked successfully. An email confirmation has been sent.").build();
    }

    @GET
    @Path("/booked")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getUserBookedEvents(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.").build());
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.").build());
        }

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.").build());
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not verified.").build());
        }

        return user.getUserDetails().getBookedEvents();
    }

    @GET
    @Path("/archived")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getArchivedEvents(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Session ID is required.").build());
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid session ID.").build());
        }

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User not found.").build());
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not verified.").build());
        }

        return user.getUserDetails().getArchivedEvents();

    }

    @GET
    public List<Event> getEvents(@QueryParam("topics") List<String> topics, @QueryParam("date") String date, @QueryParam("speakers") List<String> speakers) {
        List<Event> events = new ArrayList<>();

        if ((topics == null || topics.isEmpty()) && (date == null || date.isEmpty()) && (speakers == null || speakers.isEmpty())) {
            return eventService.getAllEvents();
        }

        if (topics != null && !topics.isEmpty()) {
            List<Event> eventsByTopic = eventService.getEventsByTopic(topics);
            if (eventsByTopic != null) {
                events.addAll(eventsByTopic);
            }
        }

        if (date != null && !date.isEmpty()) {
            List<Event> eventsByDate = eventService.getEventsByDate(date);
            if (eventsByDate != null) {
                events.addAll(eventsByDate);
            }
        }

        if (speakers != null && !speakers.isEmpty()) {
            List<Event> eventsBySpeaker = eventService.getEventsBySpeaker(speakers);
            if (eventsBySpeaker != null) {
                events.addAll(eventsBySpeaker);
            }
        }

        return events;
    }
}
