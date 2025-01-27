package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Session;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionRepository implements PanacheMongoRepository<Session> {

    public void deleteSession(Session existingSession) {
        delete(existingSession);
    }

    public void createSession(Session newSession) {
        persist(newSession);
    }
}
