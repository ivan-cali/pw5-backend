package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.HostStatus;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.service.*;
import Its.incom.pw5.service.exception.HostDeleteException;
import Its.incom.pw5.service.exception.HostNotFoundException;
import Its.incom.pw5.service.exception.HostUpdateException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.HashMap;
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

    public UserResource(UserService userService, SessionService sessionService, HostService hostService, MailService mailService, NotificationService notificationService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.hostService = hostService;
        this.mailService = mailService;
        this.notificationService = notificationService;
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

        userService.convertStringToObjectId(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    //all requests for new hosts
   /* @GET
    @Path("{id}/notification")
    public Response getHostRequests()*/

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
}
