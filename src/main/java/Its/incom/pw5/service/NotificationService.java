package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import Its.incom.pw5.persistence.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository){
        this.notificationRepository = notificationRepository;
    }

    //create a host request
    public void create(Host host, AdminNotification notification){
        if (host == null){
            throw new IllegalArgumentException("Host cannot be null");
        }

        notification.setStatus(NotificationStatus.UNREAD);
        notification.setHostEmail(host.getEmail());
        notification.setHostId(host.getId());
        notification.setTimestamp(LocalDateTime.now());
        notification.setMessage("Richisesta di creazione di un nuovo host");
        notification.setHandledBy(host.getCreatedBy());

        notificationRepository.create(notification);
    }

    public AdminNotification getById(ObjectId id){
        return notificationRepository.findNotificationById(id);
    }

    public void update(AdminNotification adminNotification){
        AdminNotification newNotification = notificationRepository.findById(adminNotification.getId());

        newNotification.setHandledBy(adminNotification.getHandledBy());
        newNotification.setStatus(NotificationStatus.HANDLED);
        notificationRepository.update(newNotification);
    }

    public List<AdminNotification> getAllNotifications(){
        return notificationRepository.getAll();
    }

    public List<AdminNotification> getFilteredNotificationByStatus(NotificationStatus status){
        return notificationRepository.getFilteredNotificationsByStatus(status);
    }
}
