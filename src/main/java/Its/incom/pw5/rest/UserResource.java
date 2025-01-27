package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.repository.UserRepository;
import Its.incom.pw5.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/user")
public class UserResource {
    private final UserRepository userRepository;
    private final UserService userService;

    public UserResource(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getUsers() {
        List<User> users = userRepository.getAll();
        return users;
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        userService.convertStringToObjectId(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
