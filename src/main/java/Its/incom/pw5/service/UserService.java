package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.persistence.repository.UserRepository;
import Its.incom.pw5.rest.model.SpeakerResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@GlobalLog
@ApplicationScoped
public class UserService {
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final HashCalculator hashCalculator;

    public UserService(UserRepository userRepository, HashCalculator hashCalculator) {
        this.userRepository = userRepository;
        this.hashCalculator = hashCalculator;
    }

    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Email is required"))
                    .build());
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid email format"))
                    .build());
        }
        return email.trim();
    }

    public void deleteUser(String id) {
        if (id == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "User id cannot be null."))
                    .build());
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

    public void updateUserToSpeaker(User user) {
        User updatedUser = userRepository.getUserByEmail(user.getEmail());
        updatedUser.setRole(Role.SPEAKER);
        userRepository.updateUser(updatedUser);
    }

    public void updateSpeakerToUser(User user) {
        User updatedUser = userRepository.getUserByEmail(user.getEmail());
        updatedUser.setRole(Role.USER);
        userRepository.updateUser(updatedUser);
    }

    public List<SpeakerResponse> getAllSpeakers() {
        return userRepository.getAllByRole(Role.SPEAKER).stream()
                .map(user -> new SpeakerResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .toList();
    }

    public List<User> findUserByFullName(String s) {
        return userRepository.findUsersByFullName(s);
    }

    public void updateUser(User user) {
        userRepository.updateUser(user);
    }

    public User checkUserCredentials(String email, String psw) {
        if (email == null || email.isBlank() || psw == null || psw.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Email and password are required"))
                    .build());
        }

        String sanitizedEmail = validateAndSanitizeEmail(email);

        String hashedPsw = hashCalculator.calculateHash(psw);
        return userRepository.getUserByCredentials(sanitizedEmail, hashedPsw);
    }
}
