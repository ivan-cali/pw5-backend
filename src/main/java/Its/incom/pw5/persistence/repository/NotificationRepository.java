package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import Its.incom.pw5.service.exception.InvalidInputException;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheMongoRepository<AdminNotification> {

    // Validate and sanitize ObjectId
    private ObjectId validateAndSanitizeObjectId(ObjectId id) {
        if (id == null) {
            throw new InvalidInputException("ID cannot be null.");
        }
        return id;
    }

    // Validate NotificationStatus input
    private NotificationStatus validateAndSanitizeStatus(NotificationStatus status) {
        if (status == null) {
            throw new InvalidInputException("Notification status cannot be null.");
        }
        return status;
    }

    // Create a new notification
    public void create(AdminNotification notification) {
        persist(notification);
    }

    // Update an existing notification
    public void update(AdminNotification notification) {
        persistOrUpdate(notification);
    }

    // Find a notification by ID
    public AdminNotification findNotificationById(ObjectId id) {
        ObjectId sanitizedId = validateAndSanitizeObjectId(id);
        return findById(sanitizedId);
    }

    // Get all notifications
    public List<AdminNotification> getAll() {
        return findAll().list();
    }

    // Get filtered notifications by status
    public List<AdminNotification> getFilteredNotificationsByStatus(NotificationStatus status) {
        NotificationStatus sanitizedStatus = validateAndSanitizeStatus(status);
        return find("status", sanitizedStatus).list();
    }
}
