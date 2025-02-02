package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.rest.model.UserBookingResponse;
import Its.incom.pw5.service.EventService;
import Its.incom.pw5.service.MailService;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.UserService;
import Its.incom.pw5.service.*;
import Its.incom.pw5.service.exception.HostNotFoundException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        String hostName;

        Host host = hostService.getHostById(session.getUserId());
        if (host == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User is not an host."))
                    .build();
        } else {
            hostName = host.getName();
        }

        User user = userService.getUserByEmail(host.getCreatedBy());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }


        if (user.getRole() == Role.ADMIN) {
            hostName = "Admin";
        }

        if (event == null) {
            // Return a bad request if no event data was provided
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event body is required."))
                    .build();
        }


        Event createdEvent = eventService.createEvent(event, hostName);

        Map<String, Object> responseBody = Map.of(
                "message", "Event created successfully.",
                "event", createdEvent
        );

        return Response.status(Response.Status.CREATED).entity(responseBody).build();
    }
    @GET
    @Path("/{id}")
    public Response getEventById(@PathParam("id") ObjectId id) {
        Event event = eventService.getEventByObjectId(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Event not found."))
                    .build();
        }

        return Response.ok(Map.of("event", event)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEvent(@PathParam("id") ObjectId id, Event updatedEvent, @QueryParam("speakerEmail") String speakerEmail) {
        try {
            // Update the event
            Event event = eventService.updateEvent(id, updatedEvent, speakerEmail);

            Map<String, Object> responseBody = Map.of(
                    "message", "Event updated successfully.",
                    "event", event
            );

            return Response.ok(responseBody).build();
        } catch (WebApplicationException ex) {
            return Response.status(ex.getResponse().getStatus())
                    .entity(Map.of("message", ex.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "An unexpected error occurred."))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEvent(@PathParam("id") ObjectId id, @CookieParam("SESSION_ID") String sessionId) {
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

        Host host = hostService.getHostById(session.getUserId());
        if (host == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User is not authorized to perform this action."))
                    .build();
        }

        // Check if the user is an admin
        User user = userService.getUserByEmail(host.getCreatedBy());
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;

        // Check if the event exists
        Event event = eventService.getEventByObjectId(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Event not found."))
                    .build();
        }

        // Allow deletion if the user is either the admin or the creator of the event
        if (!isAdmin && !event.getHost().trim().equalsIgnoreCase(host.getName().trim())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "User is not authorized to delete this event."))
                    .build();
        }


        // Proceed to delete the event
        eventService.deleteEvent(id, host);

        Map<String, Object> responseBody = Map.of(
                "message", "Event deleted successfully."
        );

        return Response.ok(responseBody).build();
    }



    @PUT
    @Path("/book")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bookEvent(@CookieParam("SESSION_ID") String sessionId, Event eventId) {
        if (sessionId == null) {
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

        Host host = hostService.getHostById(session.getUserId());
        if (host != null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Hosts cannot book events."))
                    .build();
        }
        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }


        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User is not verified."))
                    .build();
        }

        eventService.checkAndBookEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingConfirmationMail(user.getEmail(), event);

        Map<String, Object> responseBody = Map.of(
                "message", "Event booked successfully. An email confirmation has been sent.",
                "event", event
        );

        return Response.ok(responseBody).build();
    }

    @PUT
    @Path("/revoke")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response revokeEvent(@CookieParam("SESSION_ID") String sessionId, Event eventId) {
        if (sessionId == null) {
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

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User is not verified."))
                    .build();
        }

        eventService.checkAndRevokeEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingRevocationMail(user.getEmail(), event);

        Map<String, Object> responseBody = Map.of(
                "message", "Event revoked successfully. An email confirmation has been sent.",
                "event", event
        );

        return Response.ok(responseBody).build();
    }

    @GET
    @Path("/booked")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserBookedEvents(@CookieParam("SESSION_ID") String sessionId) {
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

        List<Event> bookedEvents = user.getUserDetails().getBookedEvents();
        List<Ticket> bookedTickets = user.getUserDetails().getBookedTickets();

        Map<String, Object> responseBody = Map.of(
                "message", "User booked events and tickets retrieved successfully.",
                "bookedEvents", bookedEvents,
                "bookedTickets", bookedTickets
        );

        return Response.ok(responseBody).build();
    }

    @GET
    @Path("/archived")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchivedEvents(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
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

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User is not verified."))
                    .build();
        }

        List<Event> archivedEvents = user.getUserDetails().getArchivedEvents();

        Map<String, Object> responseBody = Map.of(
                "message", "Archived events retrieved successfully.",
                "archivedEvents", archivedEvents
        );

        return Response.ok(responseBody).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("topics") List<String> topics,
                              @QueryParam("date") String date,
                              @QueryParam("speakers") List<String> speakers) {
        List<Event> events = new ArrayList<>();

        if ((topics == null || topics.isEmpty()) &&
                (date == null || date.isEmpty()) &&
                (speakers == null || speakers.isEmpty())) {
            events = eventService.getAllEvents();
        } else {
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
        }

        Map<String, Object> responseBody = Map.of(
                "message", "Events retrieved successfully.",
                "events", events
        );

        return Response.ok(responseBody).build();
    }
    @POST
    @Path("/event-admin")
    public Response createEventAsAdmin(@CookieParam("SESSION_ID") String sessionId, Event event) {
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
            System.out.println("No user found for session ID: " + session.getUserId());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }

        System.out.println("User found: " + user.getEmail() + ", Role: " + user.getRole());

        if (user.getRole() == null || !Role.ADMIN.equals(user.getRole())) {
            System.out.println("User role check failed. Expected: ADMIN, Found: " + user.getRole());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "User is not an admin."))
                    .build();
        }


        Event createdEvent = eventService.createEvent(event, "Admin");
        Map<String, Object> responseBody = Map.of(
                "message", "Event created successfully by admin.",
                "event", createdEvent
        );

        return Response.status(Response.Status.CREATED).entity(responseBody).build();
    }

    @DELETE
    @Path("/event-admin/{id}")
    public Response deleteEventAsAdmin(@PathParam("id") ObjectId id, @CookieParam("SESSION_ID") String sessionId) {
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

        User user = userService.getUserById(session.getUserId());

        if (user == null) {
            System.out.println("No user found for session ID: " + session.getUserId());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not found."))
                    .build();
        }

        System.out.println("User found: " + user.getEmail() + ", Role: " + user.getRole());

        if (user.getRole() == null || !Role.ADMIN.equals(user.getRole())) {
            System.out.println("User role check failed. Expected: ADMIN, Found: " + user.getRole());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "User is not an admin."))
                    .build();
        }

        Event event = eventService.getEventByObjectId(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Event not found."))
                    .build();
        }

        eventService.deleteEventAsAdmin(id);

        Map<String, Object> responseBody = Map.of(
                "message", "Event deleted successfully by admin."
        );

        return Response.ok(responseBody).build();
    }

}
