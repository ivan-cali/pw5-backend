package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(User user) {
        return userRepository.getUserByEmail(user.getEmail());
    }
}
