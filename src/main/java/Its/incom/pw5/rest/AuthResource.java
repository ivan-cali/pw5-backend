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
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.management.Notification;
import java.time.LocalDateTime;
import java.util.Map;
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        authService.checkNewUserCredentials(user);
        mailService.sendVerificationMail(user.getEmail());

        Map<String, Object> responseBody = Map.of(
                "message", "User successfully registered.",
                "user", user
        );

        return Response.status(Response.Status.CREATED)
                .entity(responseBody)
                .build();
    }

    @POST
    @Path("/login")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        User validUser = userService.checkUserCredentials(user.getEmail(), user.getHashedPsw());

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

        Map<String, Object> responseBody = Map.of(
                "message", "User successfully logged in.",
                "user", validUser
        );

        return Response.ok(responseBody)
                .cookie(sessionCookie)
                .build();
    }

    @PUT
    @Path("/confirm/{token}")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

        Map<String, Object> responseBody = Map.of(
                "message", "User successfully confirmed.",
                "user", user
        );

        return Response.ok(responseBody).build();
    }

    @GET
    @Path("/send-confirmation-mail")
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
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

            Map<String, Object> responseBody = Map.of(
                    "message", "Host successfully registered.",
                    "host", newHost
            );

            return Response.status(Response.Status.CREATED)
                    .entity(responseBody)
                    .build();
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
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response loginHost(Host host) {
        try {
            if (host.getEmail() == null || host.getHashedPsw() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Email and password must be provided.")
                        .build();
            }

            // Validate the login
            if (!hostService.isValidHostLogin(host.getEmail(), host.getHashedPsw())) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid credentials.")
                        .build();
            }

            Host validHost = hostService.getValidHost(host.getEmail());
            Session session = sessionService.createOrReuseSession(String.valueOf(validHost.getId()));
            String sessionCookieValue = session.getCookieValue();

            NewCookie sessionCookie = new NewCookie(
                    "SESSION_ID",
                    sessionCookieValue,
                    "/",
                    null,
                    null,
                    (int) java.time.Duration.between(LocalDateTime.now(), session.getExpiresIn()).getSeconds(),
                    false
            );

            Map<String, Object> responseBody = Map.of(
                    "message", "Host successfully logged in.",
                    "host", validHost
            );

            return Response.ok(responseBody)
                    .cookie(sessionCookie)
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        }
    }
}
