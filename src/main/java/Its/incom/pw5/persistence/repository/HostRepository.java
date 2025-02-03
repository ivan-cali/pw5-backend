package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class HostRepository implements PanacheMongoRepository<Host> {

    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-\\s]+$");
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Validates and sanitizes a name field
    private String validateAndSanitizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Name cannot be null or empty.");
        }
        if (!SAFE_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidInputException("Invalid name format.");
        }
        return name.trim();
    }

    // Validates and sanitizes an email field
    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("Email cannot be null or empty.");
        }
        if (!SAFE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException("Invalid email format.");
        }
        return email.trim();
    }

    // Get all hosts
    public List<Host> getAll() {
        return listAll();
    }

    // Create new host
    public void create(Host newHost) {
        persist(newHost);
    }

    // Delete a host
    public void delete(Host host) {
        delete(host);
    }

    //hostName already exist
    public boolean hostNameExists(String name) {
        String sanitizedName = validateAndSanitizeName(name);
        return find("name", sanitizedName).firstResult() != null;
    }

    // Check if host email exists
    public boolean hostEmailExists(String email) {
        String sanitizedEmail = validateAndSanitizeEmail(email);
        return find("email", sanitizedEmail).firstResult() != null;
    }

    // Get host by email
    public Host findByEmail(String email) {
        String sanitizedEmail = validateAndSanitizeEmail(email);
        return find("email", sanitizedEmail).firstResult();
    }

    // Get host by ID
    public Host getById(ObjectId id) {
        if (id == null) {
            throw new InvalidInputException("ID cannot be null.");
        }
        return findById(id);
    }

    // Change host status or update host
    public void updateHost(Host newHost) {
        update(newHost);
    }

    // Get host by creator's email
    public Host getByUserCreatorEmail(String email) {
        String sanitizedEmail = validateAndSanitizeEmail(email);
        return find("createdBy", sanitizedEmail).firstResult();
    }
}
