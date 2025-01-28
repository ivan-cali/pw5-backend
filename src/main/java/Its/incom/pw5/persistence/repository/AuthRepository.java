package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthRepository implements PanacheMongoRepository<User> {

    public void register(User user) {
        User u = find("email", user.getEmail()).firstResult();
        if (u != null) {
            throw new IllegalArgumentException("User already exists");
        }
        persist(user);
    }

    public void login(String email, String hashedPsw) {
        User u = find("email", email).firstResult();
        if (u == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (!u.getHashedPsw().equals(hashedPsw)) {
            throw new IllegalArgumentException("Invalid password");
        }
    }
}
