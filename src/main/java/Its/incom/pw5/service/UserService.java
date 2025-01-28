package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import Its.incom.pw5.persistence.model.User;

import java.util.List;

@ApplicationScoped
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(User user) {
        return userRepository.getUserByEmail(user.getEmail());
    }

    public void convertStringToObjectId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        ObjectId objectId = new ObjectId(id);
        userRepository.deleteUserById(objectId);
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public User getUserById(String sessionId) {
        return userRepository.getById(sessionId);
    }

    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public void confirmUser(User user) {
        user.setStatus(UserStatus.VERIFIED);
        userRepository.updateUser(user);
    }
}
