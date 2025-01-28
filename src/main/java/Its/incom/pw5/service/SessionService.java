package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.repository.SessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class SessionService {

    @Inject
    SessionRepository sessionRepository;

    public Session createOrReuseSession(String objectId) {
        // Find an existing session for the object ID
        Session existingSession = sessionRepository.find("objectId", objectId).firstResult();

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

        newSession.setUserId(objectId);

        String generatedCookieValue = UUID.randomUUID().toString();
        newSession.setCookieValue(generatedCookieValue);

        newSession.setExpiresIn(LocalDateTime.now().plusDays(7));

        sessionRepository.createSession(newSession);

        return newSession;
    }


    public Session getSessionByCookieValue(String cookieValue) {
        return sessionRepository.find("cookieValue", cookieValue).firstResult();
    }

    public boolean logout(String cookieValue) {
        // Check if the session exists
        Session existingSession = sessionRepository.find("cookieValue", cookieValue).firstResult();
        if (existingSession != null) {
            // Delete the session
            sessionRepository.delete(existingSession);
            return true;
        }
        return false; // If no session was found, return false
    }

    public String findEmailBySessionCookie(String cookieValue) {
        Session session = getSessionByCookieValue(cookieValue);
        return (session != null) ? session.getUserId() : null;
    }
}
