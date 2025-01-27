package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.repository.AuthRepository;
import Its.incom.pw5.service.AuthService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {
    private final AuthService authService;
    private final AuthRepository authRepository;

    public AuthResource(AuthService authService, AuthRepository authRepository) {
        this.authService = authService;
        this.authRepository = authRepository;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register (User user) {
        authService.checkNewUserCredentials(user);
        return Response.status(Response.Status.CREATED).entity("Utente registrato con successo").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        authService.checkUserCredentials(user);
        return Response.status(Response.Status.OK).entity("Utente loggato con successo").build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // TODO: implementare il logout con la rimozione del session cookie
        return Response.status(Response.Status.OK).entity("Utente disconnesso con successo").build();
    }

}
