package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.service.AuthService;
import Its.incom.pw5.service.SessionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;


@Path("/auth")
public class AuthResource {
    private final AuthService authService;
    private final SessionService sessionService;

    public AuthResource(AuthService authService, Its.incom.pw5.service.SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        authService.checkNewUserCredentials(user);
        return Response.status(Response.Status.CREATED).entity("User successfully registered.").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        User validUser = authService.checkUserCredentials(user);

        if (validUser == null) {
            // Authentication failed
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials.")
                    .build();
        }
        Session session = sessionService.createOrReuseSession(validUser.getEmail());
        String sessionCookieValue = session.getCookieValue();


        NewCookie sessionCookie = new NewCookie(
                "SESSION_ID",           // Cookie name
                sessionCookieValue,     // Cookie value
                "/",                    // Path
                null,                   // Domain (null uses request domain)
                null,                   // Comment
                (int) java.time.Duration.between(LocalDateTime.now(), session.getExpiresIn()).getSeconds(), // Max age in seconds
                false                   // Secure flag (true if using HTTPS)
        );

        return Response
                .ok("User successfully logged in.")
                .cookie(sessionCookie)
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("SESSION_ID") String sessionCookie) {
        if (sessionCookie == null || sessionCookie.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Session cookie is missing.")
                    .build();
        }

        boolean success = sessionService.logout(sessionCookie);
        if (success) {
            // Invalidate the cookie in the response
            return Response.status(Response.Status.OK)
                    .entity("User successfully logged out.")
                    .cookie(
                            NewCookie.valueOf("SESSION_ID=; Path=/; HttpOnly; Max-Age=0")
                    )
                    .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Session not found or already invalidated.")
                    .build();
        }
    }
}
