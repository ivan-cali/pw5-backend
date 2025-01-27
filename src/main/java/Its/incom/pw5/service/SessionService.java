package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.repository.SessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class SessionService {

    @Inject
    SessionRepository sessionRepository;

    public Session createOrReuseSession(String email) {
        // Find an existing session for the email
        Session existingSession = sessionRepository.find(
                "email = ?1",
                email
        ).firstResult();

        if (existingSession != null) {
            // Check if the session has expired
            if (existingSession.getExpiresIn().isBefore(LocalDateTime.now())) {
                // Delete the expired session
                sessionRepository.deleteSession(existingSession);
            } else {
                // Return the existing session if it is still valid
                return existingSession;
            }
        }

        // Create a new session
        Session newSession = new Session();
        newSession.setEmail(email);

        String generatedCookieValue = UUID.randomUUID().toString();
        newSession.setCookieValue(generatedCookieValue);

        newSession.setExpiresIn(LocalDateTime.now().plusDays(7));

        sessionRepository.createSession(newSession);

        return newSession;
    }


    public Session getSessionByCookieValue(String cookieValue) {
        return sessionRepository.find("cookieValue", cookieValue).firstResult();
    }

    public void deleteSession(String cookieValue) {
        Session existing = sessionRepository.find("cookieValue", cookieValue).firstResult();
        if (existing != null) {
            sessionRepository.delete(existing);
        }
    }

    public String findEmailBySessionCookie(String cookieValue) {
        Session session = getSessionByCookieValue(cookieValue);
        return (session != null) ? session.getEmail() : null;
    }
}
