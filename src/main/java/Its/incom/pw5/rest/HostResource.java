package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.rest.model.PasswordEditRequest;
import Its.incom.pw5.service.HostService;
import Its.incom.pw5.service.MailService;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.UserService;
import Its.incom.pw5.service.exception.HostNotFoundException;
import Its.incom.pw5.service.exception.HostUpdateException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.Map;


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
