package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserRepository implements PanacheMongoRepository<User> {

    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[A-Za-z\\s]+$");

    // Validate and sanitize ObjectId
    private ObjectId validateAndSanitizeObjectId(String id) {
        if (id == null || id.isBlank() || !ObjectId.isValid(id)) {
            throw new InvalidInputException("Invalid ObjectId.");
        }
        return new ObjectId(id);
    }

    // Validate and sanitize email
    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("Email cannot be null or empty.");
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException("Invalid email format.");
        }
        return email.trim();
    }

    // Validate and sanitize full name
    private String[] validateAndSanitizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidInputException("Full name cannot be null or empty.");
        }
        if (!SAFE_NAME_PATTERN.matcher(fullName).matches()) {
            throw new InvalidInputException("Invalid full name format.");
        }

        String[] names = fullName.trim().split("\\s+", 2);
        if (names.length < 2) {
            throw new InvalidInputException("Full name must include both first and last name.");
        }
        return names;
    }

    public List<User> getAll() {
        return listAll();
    }

    public void deleteUserById(ObjectId id) {
        validateAndSanitizeObjectId(id.toString());
        deleteById(id);
    }

    public User getUserByEmail(String email) {
        String sanitizedEmail = validateAndSanitizeEmail(email);
        return find("email", sanitizedEmail).firstResult();
    }

    public List<User> getAllUsers() {
        return listAll();
    }

    public User getById(String userId) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(userId);
        return findById(sanitizedId);
    }

    public List<User> getAllByRole(Role role) {
        if (role == null) {
            throw new InvalidInputException("Role cannot be null.");
        }
        return find("role", role).list();
    }

    public void updateUser(User updatedUser) {
        update(updatedUser);
    }

    public List<User> findUsersByFullName(String fullName) {
        String[] sanitizedNames = validateAndSanitizeFullName(fullName);
        return find("firstName = ?1 and lastName = ?2", sanitizedNames[0], sanitizedNames[1]).list();
    }

    public User getUserByCredentials(String email, String hashedPsw) {
        String sanitizedEmail = validateAndSanitizeEmail(email);

        if (hashedPsw == null || hashedPsw.isBlank()) {
            throw new InvalidInputException("Password cannot be null or empty.");
        }

        return find("email = ?1 and hashedPsw = ?2", sanitizedEmail, hashedPsw).firstResult();
    }
}
