package Its.incom.pw5.service;

import Its.incom.pw5.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import Its.incom.pw5.persistence.model.User;

@ApplicationScoped
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void convertStringToObjectId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        ObjectId objectId = new ObjectId(id);
        userRepository.deleteUserById(objectId);
    }

    public User getUser(User user) {
        return userRepository.getUserByEmail(user.getEmail());
    }
}
