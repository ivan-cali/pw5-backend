package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.Role;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheMongoRepository<User> {
    public List<User> getAll() {
        return listAll();
    }

    public void deleteUserById(ObjectId id) {
        deleteById(id);
    }

    public User getUserByEmail(String email) {
        return find("email", email).firstResult();
    }

    public List<User> getAllUsers() {
        return listAll();
    }

    public User getById(String userId) {
        return findById(new ObjectId(userId));
    }

    public List<User> getAllByRole(Role role) {
        return find("role", role).list();
    }

    public void updateUser(User user) {
        update(user);
    }
}
