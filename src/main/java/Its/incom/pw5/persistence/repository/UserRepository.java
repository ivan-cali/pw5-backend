package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheMongoRepository<User> {

    public User getUserByEmail(String email) {
        return find("email", email).firstResult();
    }
}
