package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.rest.model.SpeakerResponse;
import Its.incom.pw5.service.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/user")
public class UserResource {
    private final UserService userService;
    private final SessionService sessionService;
    private final HostService hostService;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final TopicService topicService;
    private final HashCalculator hashCalculator;

    public UserResource(UserService userService, HashCalculator hashCalculator, SessionService sessionService, HostService hostService, MailService mailService, NotificationService notificationService, TopicService topicService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.hashCalculator = hashCalculator;
        this.hostService = hostService;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.topicService = topicService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getUsers(@CookieParam("SESSION_ID") String sessionId) {
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

        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }

        List<User> userList = userService.getAllUsers();

        Map<String, Object> responseBody = Map.of(
                "message", "Users retrieved successfully.",
                "users", userList
        );

        return Response.ok(responseBody)
                .build();
    }


    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response deleteUser(@PathParam("id") String id, @CookieParam("SESSION_ID") String sessionId) {
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

        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }
        // Check if the user to delete exists
        User userToDelete = userService.getUserById(id);
        if (userToDelete == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "User with ID " + id + " not found."))
                    .build();
        }

        userService.deleteUser(id);

        Map<String, Object> responseBody = Map.of(
                "message", "User deleted successfully."
        );

        return Response.ok(responseBody)
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response updateUserToSpeaker(@CookieParam("SESSION_ID") String sessionId) {
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

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "User not verified."))
                    .build();
        }

        if (Role.ADMIN == user.getRole()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "Admins cannot change their role."))
                    .build();
        }

        if (Role.SPEAKER == user.getRole()) {
            userService.updateSpeakerToUser(user);
            user = userService.getUserById(user.getId().toHexString()); // Fetch updated user object

            Map<String, Object> responseBody = Map.of(
                    "message", "Speaker updated to User successfully.",
                    "user", user
            );

            return Response.ok(responseBody)
                    .build();
        } else {
            userService.updateUserToSpeaker(user);
            user = userService.getUserById(user.getId().toHexString()); // Fetch updated user object

            Map<String, Object> responseBody = Map.of(
                    "message", "User updated to Speaker successfully.",
                    "user", user
            );

            return Response.ok(responseBody)
                    .build();
        }
    }

    @GET
    @Path("/speakers")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getSpeakers() {
        List<SpeakerResponse> speakers = userService.getAllSpeakers();

        Map<String, Object> responseBody = Map.of(
                "message", "Speakers retrieved successfully.",
                "speakers", speakers
        );

        return Response.ok(responseBody)
                .build();
    }

    //all notifications for new host creation
    @GET
    @Path("/notification/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getHostRequests(@CookieParam("SESSION_ID") String sessionId) {
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
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }

        List<AdminNotification> notifications = notificationService.getAllNotifications();

        Map<String, Object> responseBody = Map.of(
                "message", "Notifications retrieved successfully.",
                "notifications", notifications
        );

        return Response.ok(responseBody)
                .build();
    }

    //all notification filtered by status
    @GET
    @Path("/notification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getUnreadNotifications(@CookieParam("SESSION_ID") String sessionId, @QueryParam("status") NotificationStatus status) {
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
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }

        List<AdminNotification> notifications = notificationService.getFilteredNotificationByStatus(status);

        Map<String, Object> responseBody = Map.of(
                "message", "Filtered notifications retrieved successfully.",
                "notifications", notifications
        );

        return Response.ok(responseBody)
                .build();
    }


    //new host approval by admin
    @PUT
    @Path("/notification/{notificationId}/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response approveRequest(@CookieParam("SESSION_ID") String sessionId, @PathParam("notificationId") ObjectId notificationId) {
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
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }

        AdminNotification notification = notificationService.getById(notificationId);
        if (notification == null || !notification.getStatus().equals(NotificationStatus.UNREAD)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Notification not found or already handled."))
                    .build();
        }

        Host hostRequest = hostService.findHostRequst(notification.getHostId());
        String generatedPsw = UUID.randomUUID().toString();

        String hashedPsw = hashCalculator.calculateHash(generatedPsw);
        hostRequest.setProvvisoryPsw(hashedPsw);  // Set the hashed temporary password

        hostService.update(hostRequest, generatedPsw);

        mailService.sendHostRequestApprovalEmail(hostRequest.getEmail(), generatedPsw);

        notificationService.update(notification);

        Map<String, Object> responseBody = Map.of(
                "message", "Notification confirmed and host request approved successfully.",
                "hostRequest", hostRequest
        );

        return Response.ok(responseBody)
                .build();
    }

    //new host rejection by admin
    @PUT
    @Path("/notification/{notificationId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response rejectRequest(@CookieParam("SESSION_ID") String sessionId, @PathParam("notificationId") ObjectId notificationId) {
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
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Logged user is not an admin."))
                    .build();
        }

        AdminNotification notification = notificationService.getById(notificationId);
        if (notification == null || !notification.getStatus().equals(NotificationStatus.UNREAD)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Notification not found or already handled."))
                    .build();
        }

        Host hostRequest = hostService.findHostRequst(notification.getHostId());

        hostService.rejectHostRequest(hostRequest);

        //email di non conferma
        mailService.sendHostRequestRejectionEmail(hostRequest.getEmail());


        notificationService.update(notification);

        Map<String, Object> responseBody = Map.of(
                "message", "Notification rejected and host request handled successfully.",
                "hostRequest", hostRequest
        );

        return Response.ok(responseBody)
                .build();
    }

    //Add a topic to user favourite topic list
    @PUT
    @Path("/favourite-topic/add/{topicId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response addFavouriteTopic(@CookieParam("SESSION_ID") String sessionId, @PathParam("topicId") ObjectId topicId) {
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

        Topic topic = topicService.getTopicById(topicId);
        if (topic == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Topic not found."))
                    .build();
        }

        topicService.addFavouriteTopic(user, topic);
        User updatedUser = userService.getUserById(user.getId().toHexString());


        Map<String, Object> responseBody = Map.of(
                "message", "Topic added to favourite topics successfully.",
                "user", updatedUser
        );

        return Response.ok(responseBody)
                .build();
    }

    //Remove a topic to user favourite topic list
    @PUT
    @Path("/favourite-topic/remove/{topicId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response removeFavouriteTopic(@CookieParam("SESSION_ID") String sessionId, @PathParam("topicId") ObjectId topicId) {
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
        Topic topic = topicService.getTopicById(topicId);
        if (topic == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Topic not found."))
                    .build();
        }

        topicService.removeFavouriteTopic(user, topic);

        User updatedUser = userService.getUserById(user.getId().toHexString());

        Map<String, Object> responseBody = Map.of(
                "message", "Topic removed from favourite topics successfully.",
                "user", updatedUser
        );

        return Response.ok(responseBody)
                .build();
    }

    //Get all user favourite topic
    @GET
    @Path("/favourite-topic")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getUserFavouriteTopics(@CookieParam("SESSION_ID") String sessionId) {
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
        List<Topic> userFavouriteTopics = user.getUserDetails().getFavouriteTopics();

        Map<String, Object> responseBody = Map.of(
                "message", "Favourite topics retrieved successfully.",
                "favouriteTopics", userFavouriteTopics
        );

        return Response.ok(responseBody)
                .build();
    }
}
