package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.UserDetails;
import Its.incom.pw5.persistence.model.VerificationToken;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.persistence.repository.AuthRepository;
import Its.incom.pw5.persistence.repository.VerificationTokenRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@GlobalLog
@ApplicationScoped
public class AuthService {
    private static final Pattern SAFE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AuthRepository authRepository;
    private final HashCalculator hashCalculator;
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthService(AuthRepository authRepository, HashCalculator hashCalculator, VerificationTokenRepository verificationTokenRepository) {
        this.authRepository = authRepository;
        this.verificationTokenRepository = verificationTokenRepository;
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


    public void checkNewUserCredentials(User user) {
        String hashedPsw = hashCalculator.calculateHash(user.getHashedPsw());

        if (user.getFirstName() == null || user.getLastName().isBlank() || user.getLastName() == null || user.getFirstName().isBlank() || user.getEmail() == null || user.getEmail().isBlank() || hashedPsw == null || hashedPsw.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields"))
                    .build());
        }

        String sanitizedEmail = validateAndSanitizeEmail(user.getEmail());

        User newUser = new User();
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setEmail(sanitizedEmail);
        newUser.setHashedPsw(hashedPsw);
        newUser.setStatus(UserStatus.UNVERIFIED);
        newUser.setRole(Role.USER);
        UserDetails newUserDetails = new UserDetails();
        newUserDetails.setArchivedEvents(new ArrayList<>());
        newUserDetails.setBookedEvents(new ArrayList<>());
        newUserDetails.setFavouriteTopics(new ArrayList<>());
        newUser.setUserDetails(newUserDetails);

        authRepository.register(newUser);
    }

    public void onStartup(@Observes StartupEvent event) {
        System.out.println("Application started, running archivePastEvents now...");
        removeExpiredVerificationTokens();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void removeExpiredVerificationTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<VerificationToken> expiredTokens = verificationTokenRepository.findExpiredTokens(now);

        if (expiredTokens.isEmpty()) {
            System.out.println("No expired verification tokens were found during the latest cleanup.");
            return;
        }

        // Collect information about deleted tokens
        StringBuilder deletedTokensInfo = new StringBuilder();
        expiredTokens.forEach(token -> {
            verificationTokenRepository.deleteToken(token);
            deletedTokensInfo.append(String.format("Deleted token for email: %s%n", token.getEmail()));
        });

        // Prepare and print the notification message
        String subject = "Verification Token Cleanup Completed";
        String body = String.format("Cleanup complete. Removed %d expired verification tokens.%n%nDetails:%n%s",
                expiredTokens.size(),
                deletedTokensInfo);
        System.out.println(subject);
        System.out.println(body);
    }
}
