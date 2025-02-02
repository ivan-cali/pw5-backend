package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.VerificationToken;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class VerificationTokenRepository implements PanacheMongoRepository<VerificationToken> {

    private static final Pattern SAFE_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    // Validate and sanitize token string
    private String validateAndSanitizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidInputException("Token cannot be null or empty.");
        }
        if (!SAFE_TOKEN_PATTERN.matcher(token).matches()) {
            throw new InvalidInputException("Invalid token format.");
        }
        return token.trim();
    }

    // Validate date input
    private LocalDateTime validateAndSanitizeDate(LocalDateTime date) {
        if (date == null) {
            throw new InvalidInputException("Date cannot be null.");
        }
        return date;
    }

    public void createToken(VerificationToken token) {
        persist(token);
    }

    public VerificationToken findByToken(String token) {
        String sanitizedToken = validateAndSanitizeToken(token);
        return find("token", sanitizedToken).firstResult();
    }

    public void deleteToken(VerificationToken token) {
        delete(token);
    }

    public List<VerificationToken> findExpiredTokens(LocalDateTime now) {
        LocalDateTime sanitizedDate = validateAndSanitizeDate(now);
        return find("expirationDate < ?1", sanitizedDate).list();
    }
}
