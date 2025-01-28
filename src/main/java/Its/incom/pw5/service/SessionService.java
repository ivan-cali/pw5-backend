package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Session;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.repository.SessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class SessionService {

    @Inject
    SessionRepository sessionRepository;

    @Inject
    UserService userService;

    public Session createOrReuseSession(String objectId) {
        // Debugging: Log the objectId being searched
        System.out.println("Searching for existing session with objectId: " + objectId);

        // Find an existing session for the object ID
        Session existingSession = sessionRepository.find("userId", objectId).firstResult();

        if (existingSession != null) {
            System.out.println("Existing session found: " + existingSession);

            // Check if the session is valid and not older than 7 days
            if (existingSession.getExpiresIn() != null) {
                if (existingSession.getExpiresIn().isAfter(LocalDateTime.now())) {
                    System.out.println("Existing session is still valid. Reusing session.");
                    return existingSession;
                } else if (existingSession.getExpiresIn().isBefore(LocalDateTime.now().minusDays(7))) {
                    System.out.println("Existing session is older than 7 days. Deleting session.");
                    sessionRepository.deleteSession(existingSession);
                } else {
                    System.out.println("Existing session has expired. Deleting session.");
                    sessionRepository.deleteSession(existingSession);
                }
            }
        } else {
            System.out.println("No existing session found for objectId: " + objectId);
        }

        // Create a new session if no valid session exists or the session is older than 7 days
        System.out.println("Creating a new session for objectId: " + objectId);
        Session newSession = new Session();
        newSession.setUserId(objectId);

        String generatedCookieValue = UUID.randomUUID().toString();
        newSession.setCookieValue(generatedCookieValue);

        newSession.setExpiresIn(LocalDateTime.now().plusDays(7));

        sessionRepository.createSession(newSession);
        System.out.println("New session created: " + newSession);

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

    public String findIdBySessionCookie(String cookieValue) {
        Session session = getSessionByCookieValue(cookieValue);
        return (session != null) ? session.getUserId() : null;
    }

    public String findEmailBySessionCookie(String cookieValue) {
        // Find the session based on the cookie value
        Session session = getSessionByCookieValue(cookieValue);
        if (session == null) {
            return null;
        }

        // Use the userId to fetch the User object and return the email
        User user = userService.getUserById(session.getUserId());
        return (user != null) ? user.getEmail() : null;
    }

    public Session getSession(String sessionId) {
        return sessionRepository.find("cookieValue", sessionId).firstResult();
    }
}
