package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/user")
public class UserResource {
    private final UserService userService;
    private final SessionService sessionService;

    public UserResource(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
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
}
