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
        Session existingSession = sessionRepository.find(
                "email = ?1 and expiresIn > ?2",
                email,
                LocalDateTime.now()
        ).firstResult();

        if (existingSession != null) {
            return existingSession;
        }

        Session newSession = new Session();
        newSession.setEmail(email);

        String generatedCookieValue = UUID.randomUUID().toString();
        newSession.setCookieValue(generatedCookieValue);

        newSession.setExpiresIn(LocalDateTime.now().plusDays(7));

        sessionRepository.persist(newSession);

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
