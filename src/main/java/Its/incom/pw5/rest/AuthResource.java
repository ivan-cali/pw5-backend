package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.VerificationToken;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.service.AuthService;
import Its.incom.pw5.service.MailService;
import Its.incom.pw5.service.SessionService;
import Its.incom.pw5.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;


@Path("/auth")
public class AuthResource {
    private final AuthService authService;
    private final SessionService sessionService;
    private final UserService userService;
    private final MailService mailService;

    public AuthResource(AuthService authService, SessionService sessionService, UserService userService, MailService mailService) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.mailService = mailService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        authService.checkNewUserCredentials(user);
        mailService.sendVerificationMail(user.getEmail());

        return Response.status(Response.Status.CREATED).entity("User successfully registered.").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        User validUser = userService.getUser(user);

        if (validUser == null) {
            // Authentication failed
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials.")
                    .build();
        }
        Session session = sessionService.createOrReuseSession(String.valueOf(validUser.getId()));
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

    @PUT
    @Path("/confirm/{token}")
    public Response confirm(@PathParam("token") String token) {
        VerificationToken verificationToken = mailService.getVerificationToken(token);
        if (verificationToken == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Token not found.")
                    .build();
        }

        User user = userService.getUserByEmail(verificationToken.getEmail());
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found.")
                    .build();
        }

        userService.confirmUser(user);

        return Response.status(Response.Status.OK)
                .entity("User successfully confirmed.")
                .build();
    }

    @GET
    @Path("/sendConfirmationMail")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendConfirmationMail(@CookieParam("SESSION_ID") String sessionId) {
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

        if (user.getStatus() == UserStatus.VERIFIED) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User is already verified.")
                    .build();
        }

        mailService.sendVerificationMail(user.getEmail());
        return Response.status(Response.Status.OK)
                .entity("Confirmation mail sent.")
                .build();
    }


    @DELETE
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
