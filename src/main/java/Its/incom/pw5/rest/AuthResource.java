package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.service.AuthService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {
    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register (User user) {
        authService.checkNewUserCredentials(user);
        return Response.status(Response.Status.CREATED).entity("User registered successfully").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        authService.checkUserCredentials(user);
        return Response.status(Response.Status.OK).entity("User logged in successfully").build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // TODO: implementare il logout con la rimozione del session cookie
        return Response.status(Response.Status.OK).entity("User logged out successfully").build();
    }

}
