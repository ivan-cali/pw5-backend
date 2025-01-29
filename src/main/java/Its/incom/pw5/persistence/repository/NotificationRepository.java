package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

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

    public List<AdminNotification> getAll(){
        return findAll().list();
    }

    public List<AdminNotification> getFilteredNotificationsByStatus(NotificationStatus status){
        return find("status", status).list();
    }
}
