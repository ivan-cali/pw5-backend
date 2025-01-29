package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class NotificationRepository implements PanacheMongoRepository<AdminNotification> {

    public void create(AdminNotification notification){
        persist(notification);
    }

    public void update(AdminNotification notification){
        persistOrUpdate(notification);
    }

    public AdminNotification findNotificationById(ObjectId id) {
        return findById(id);
    }
}
