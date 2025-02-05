package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@ApplicationScoped
public class AuthRepository implements PanacheMongoRepository<User> {

    public void register(User user) {
        // Check if the user already exists
        if (find("email", user.getEmail()).firstResult() != null) {
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "User already exists"))
                    .build());
        }

        // Persist the user
        persist(user);
    }

    public void login(String email, String hashedPsw) {

        // Check if the user exists
        User user = find("email", email).firstResult();
        if (user == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "User not found"))
                    .build());
        }

        // Validate the password
        if (!user.getHashedPsw().equals(hashedPsw)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid credentials"))
                    .build());
        }
    }
}
