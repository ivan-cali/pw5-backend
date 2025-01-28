package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.VerificationToken;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VerificationTokenRepository implements PanacheMongoRepository<VerificationToken> {
    public void createToken(VerificationToken token) {
        persist(token);
    }

    public VerificationToken findByToken(String token) {
        return find("token", token).firstResult();
    }
}
