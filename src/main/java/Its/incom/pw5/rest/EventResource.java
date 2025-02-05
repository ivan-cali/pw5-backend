package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.service.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response createEvent(@CookieParam("SESSION_ID") String sessionId, Event event) {
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

        // Get the user from the session
        User user = userService.getUserById(session.getUserId());
        Host host;
        if (user == null) {
            // Get the host from the session userId which is the host id
            host = hostService.getHostById(session.getUserId());
        } else {
            if (UserStatus.VERIFIED != user.getStatus()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "User is not verified."))
                        .build();
            }

            // Get the host from the user
            host = hostService.getHostByUserCreatorEmail(user.getEmail());
        }

        if (host == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Host not found."))
                    .build();
        }

        Event createdEvent = eventService.createEvent(event, host.getName());

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("message", "Event created successfully.", "event", createdEvent))
                .build();
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response updateEventAsHost(@PathParam("id") ObjectId id, Event updatedEvent, @QueryParam("speakerEmail") String speakerEmail, @CookieParam("SESSION_ID") String sessionId) {
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

        // Get the user from the session
        User user = userService.getUserById(session.getUserId());
        Host host;
        if (user == null) {
            // Get the host from the session userId which is the host id
            host = hostService.getHostById(session.getUserId());
        } else {
            if (UserStatus.VERIFIED != user.getStatus()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "User is not verified."))
                        .build();
            }

            // Get the host from the user
            host = hostService.getHostByUserCreatorEmail(user.getEmail());
        }

        if (host == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Host not found."))
                    .build();
        }

        Event event = eventService.getEventByObjectId(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Event not found."))
                    .build();
        }

        // Allow update if the user is the creator of the event
        if (!event.getHost().trim().equalsIgnoreCase(host.getName().trim())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "User is not authorized to update this event."))
                    .build();
        }

        eventService.updateEvent(id, updatedEvent);

        return Response.ok(Map.of("message", "Event updated successfully."))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        // Get the user from the session
        User user = userService.getUserById(session.getUserId());
        Host host;
        boolean isAdmin = false;
        if (user == null) {
            // Get the host from the session userId which is the host id
            host = hostService.getHostById(session.getUserId());
        } else {
            // Check if the user is an admin
            isAdmin = user.getRole() == Role.ADMIN;

            // Get the host from the user
            host = hostService.getHostByUserCreatorEmail(user.getEmail());

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
        }

        if (host == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Host not found."))
                    .build();
        }


        // Proceed to delete the event
        eventService.deleteEvent(id, host, isAdmin);

        Map<String, Object> responseBody = Map.of(
                "message", "Event deleted successfully."
        );

        return Response.ok(responseBody)
                .build();
    }

    @PUT
    @Path("/book")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bookEvent(@CookieParam("SESSION_ID") String sessionId, Map<String, String> body) {
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

        String id = body.get("id");

        ObjectId eventId = new ObjectId(id);

        eventService.checkAndBookEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingConfirmationMail(user.getEmail(), event);

        Map<String, Object> responseBody = Map.of(
                "message", "Event booked successfully. An email confirmation has been sent.",
                "event", event
        );

        return Response.ok(responseBody)
                .build();
    }

    @PUT
    @Path("/revoke")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response revokeEvent(@CookieParam("SESSION_ID") String sessionId, Map<String, String> body) {
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

        String id = body.get("id");

        ObjectId eventId = new ObjectId(id);

        eventService.checkAndRevokeEvent(eventId, user);

        Event event = eventService.getEventById(eventId);

        mailService.sendBookingRevocationMail(user.getEmail(), event);

        Map<String, Object> responseBody = Map.of(
                "message", "Event revoked successfully. An email confirmation has been sent.",
                "event", event
        );

        return Response.ok(responseBody)
                .build();
    }

    @GET
    @Path("/booked")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        return Response.ok(responseBody)
                .build();
    }

    @GET
    @Path("/archived")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        return Response.ok(responseBody)
                .build();
    }

    @GET
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("topics") List<String> topics, @QueryParam("date") String date, @QueryParam("speakers") List<String> speakers) {
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

        return Response.ok(responseBody)
                .build();
    }

    @POST
    @Path("/admin")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        return Response.status(Response.Status.CREATED)
                .entity(responseBody)
                .build();
    }

    @PUT
    @Path("/admin/{id}")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response updateEventAsAdmin(@PathParam("id") ObjectId id, Event updatedEvent, @CookieParam("SESSION_ID") String sessionId) {
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

        eventService.updateEventAsAdmin(id, updatedEvent);

        Map<String, Object> responseBody = Map.of(
                "message", "Event updated successfully by admin."
        );

        return Response.ok(responseBody)
                .build();
    }

    @DELETE
    @Path("/event/{id}")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        return Response.ok(responseBody)
                .build();
    }
}
