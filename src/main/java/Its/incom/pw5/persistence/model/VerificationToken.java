package Its.incom.pw5.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@MongoEntity(collection = "verificationToken")
public class VerificationToken {
    private ObjectId id;
    private String email;
    private String token;
    private LocalDateTime expirationDate;
}
