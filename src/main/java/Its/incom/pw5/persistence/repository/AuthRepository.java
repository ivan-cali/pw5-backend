package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.service.exception.UserAlreadyExistsException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.regex.Pattern;

@ApplicationScoped
public class AuthRepository implements PanacheMongoRepository<User> {

    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        return email.trim();
    }

    public void register(User user) {
        String sanitizedEmail = validateAndSanitizeEmail(user.getEmail());

        // Check if the user already exists
        if (find("email", sanitizedEmail).firstResult() != null) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        // Persist the user
        persist(user);
    }

    public void login(String email, String hashedPsw) {
        String sanitizedEmail = validateAndSanitizeEmail(email);

        // Check if the user exists
        User user = find("email", sanitizedEmail).firstResult();
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        // Validate the password
        if (!user.getHashedPsw().equals(hashedPsw)) {
            throw new IllegalArgumentException("Invalid password.");
        }
    }
}
