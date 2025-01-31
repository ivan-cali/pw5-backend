package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.Type;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.service.*;
import Its.incom.pw5.service.exception.HostAlreadyExistsException;
import Its.incom.pw5.service.exception.HostCreationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import javax.management.Notification;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;


@Path("/auth")
public class AuthResource {
    private final AuthService authService;
    private final SessionService sessionService;
    private final UserService userService;
    private final MailService mailService;
    private final HostService hostService;
    private final NotificationService notificationService;

    public AuthResource(AuthService authService, SessionService sessionService, UserService userService, MailService mailService, HostService hostService, NotificationService notificationService) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.mailService = mailService;
        this.hostService = hostService;
        this.notificationService = notificationService;
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
        User validUser = userService.getUserByEmail(user.getEmail());

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
    @Path("/send-confirmation-mail")
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

    //create a new host
    @POST
    @Path("/register-host")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@CookieParam("SESSION_ID") String sessionId, Host host) {
        try {
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Session cookie not found.").build();
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid session cookie.").build();
            }

            // Check if sessions provides a Host
            if (hostService.getHostById(session.getUserId()) != null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Host cannot create a new host.").build();
            }

            hostService.create(session.getUserId(), host);

            //notification of the new host creation request
            Host newHost = hostService.getHostByEmail(host.getEmail());
            AdminNotification notification = new AdminNotification();
            notificationService.create(newHost, notification);

            return Response.status(Response.Status.CREATED).entity("Host successfully registered.").build();
        } catch (HostAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (HostCreationException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    // login as host
    @POST
    @Path("/login-host")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loginHost(Host host) {
        Host validHost = hostService.getHostByEmail(host.getEmail());

        if (validHost == null) {
            // Authentication failed
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid credentials.")
                    .build();
        }
        if (hostService.isPasswordMatching(host.getHashedPsw(), host.getProvvisoryPsw())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Please change your password.")
                    .build();
        }

        Session session = sessionService.createOrReuseSession(String.valueOf(validHost.getId()));
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
                .ok("Host successfully logged in.")
                .cookie(sessionCookie)
                .build();
    }
}
