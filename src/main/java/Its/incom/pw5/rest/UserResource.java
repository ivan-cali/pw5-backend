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

import java.util.List;
import java.util.UUID;

@Path("/user")
public class UserResource {
    private final UserService userService;
    private final SessionService sessionService;
    private final HostService hostService;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final TopicService topicService;

    public UserResource(UserService userService, SessionService sessionService, HostService hostService, MailService mailService, NotificationService notificationService, TopicService topicService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.hostService = hostService;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.topicService = topicService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getUsers(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build());
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build());
        }

        User user = userService.getUserById(session.getUserId());
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("User not found.").build());
        }

        if (Role.ADMIN != user.getRole()) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build());
        }

        return userService.getAllUsers();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id, @CookieParam("SESSION_ID") String sessionId) {
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

        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build();
        }

        userService.deleteUser(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUserToSpeaker(@CookieParam("SESSION_ID") String sessionId) {
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

        if (UserStatus.VERIFIED != user.getStatus()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("User not verified.").build();
        }

        if (Role.SPEAKER == user.getRole()) {
            userService.updateSpeakerToUser(user);
            return Response.status(Response.Status.OK).entity("Speaker updated to User.").build();
        } else {
            userService.updateUserToSpeaker(user);
            return Response.status(Response.Status.OK).entity("User updated to Speaker.").build();
        }
    }
    @GET
    @Path("/speakers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SpeakerResponse> getSpeakers() {
        return userService.getAllSpeakers();
    }

    //all notifications for new host creation
    @GET
    @Path("/notification/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHostRequests(@CookieParam("SESSION_ID") String sessionId){
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build();
        }

        List<AdminNotification> notifications = notificationService.getAllNotifications();
        return Response.ok().entity(notifications).build();
    }

    //all notification filtered by status
    @GET
    @Path("/notification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnreadNotifications(@CookieParam("SESSION_ID") String sessionId, @QueryParam("status") NotificationStatus status){
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build();
        }

        List<AdminNotification> notifications = notificationService.getFilteredNotificationByStatus(status);
        return Response.ok().entity(notifications).build();
    }



    //new host approval by admin
    @PUT
    @Path("/notification/{notificationId}/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approveRequest(@CookieParam("SESSION_ID") String sessionId, @PathParam("notificationId") ObjectId notificationId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build();
        }

        AdminNotification notification = notificationService.getById(notificationId);
        if (notification == null || !notification.getStatus().equals(NotificationStatus.UNREAD)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Notification not found or already handled.").build();
        }

        Host hostRequest = hostService.findHostRequst(notification.getHostId());
        String generatedPsw = UUID.randomUUID().toString();

        hostRequest.setProvvisoryPsw(generatedPsw);

        hostService.update(hostRequest, generatedPsw);

        mailService.sendHostRequestApprovalEmail(hostRequest.getEmail(), generatedPsw);

        notificationService.update(notification);

        return Response.ok().build();
    }

    //new host rejection by admin
    @PUT
    @Path("/notification/{notificationId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response rejectRequest(@CookieParam("SESSION_ID") String sessionId, @PathParam("notificationId") ObjectId notificationId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        if (Role.ADMIN != user.getRole()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Logged user is not an admin.").build();
        }

        AdminNotification notification = notificationService.getById(notificationId);
        if (notification == null || !notification.getStatus().equals(NotificationStatus.UNREAD)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Notification not found or already handled.").build();
        }

        Host hostRequest = hostService.findHostRequst(notification.getHostId());

        hostService.rejectHostRequest(hostRequest);

        //email di non conferma
        mailService.sendHostRequestRejectionEmail(hostRequest.getEmail());


        notificationService.update(notification);

        return Response.ok().build();
    }

    //Add a topic to user favourite topic list
    @PUT
    @Path("/favourite-topic/add/{topicId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFavouriteTopic(@CookieParam("SESSION_ID") String sessionId, @PathParam("topicId") ObjectId topicId){
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());

        Topic topic = topicService.getTopicById(topicId);
        if (topic == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Topic not found.").build();
        }

        topicService.addFavouriteTopic(user, topic);
        return Response.ok().entity(topic.getName() + " added to favourite topics.").build();
    }

    //Remove a topic to user favourite topic list
    @PUT
    @Path("/favourite-topic/remove/{topicId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFavouriteTopic(@CookieParam("SESSION_ID") String sessionId, @PathParam("topicId") ObjectId topicId){
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        Topic topic = topicService.getTopicById(topicId);
        if (topic == null){
            return Response.status(Response.Status.NOT_FOUND).entity("Topic not found.").build();
        }

        topicService.removeFavouriteTopic(user, topic);
        return Response.ok().entity(topic.getName() + " removed from favourite topics.").build();
    }

    //Get all user favourite topic
    @GET
    @Path("/favourite-topic")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserFavouriteTopics(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
        }

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
        }

        User user = userService.getUserById(session.getUserId());
        List<Topic> userFavouriteTopics = user.getUserDetails().getFavouriteTopics();
        return Response.ok(userFavouriteTopics).build();
    }
}
