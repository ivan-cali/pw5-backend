package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.regex.Pattern;

@ApplicationScoped
public class SessionRepository implements PanacheMongoRepository<Session> {

    private static final Pattern SAFE_COOKIE_VALUE_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    // Validate and sanitize cookie value
    private String validateAndSanitizeCookieValue(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            throw new InvalidInputException("Cookie value cannot be null or empty.");
        }
        if (!SAFE_COOKIE_VALUE_PATTERN.matcher(cookieValue).matches()) {
            throw new InvalidInputException("Invalid cookie value format.");
        }
        return cookieValue.trim();
    }

    // Delete an existing session
    public void deleteSession(Session existingSession) {
        delete(existingSession);
    }

    // Create a new session
    public void createSession(Session newSession) {
        persist(newSession);
    }

    // Delete session by validated cookie value
    public void deleteByCookieValue(String cookieValue) {
        String sanitizedCookieValue = validateAndSanitizeCookieValue(cookieValue);
        delete("cookieValue", sanitizedCookieValue);
    }
}
