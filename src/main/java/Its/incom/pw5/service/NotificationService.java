package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.AdminNotification;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.enums.NotificationStatus;
import Its.incom.pw5.persistence.repository.NotificationRepository;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

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

        notificationRepository.create(notification);
    }

    public AdminNotification getById(ObjectId id){
        return notificationRepository.findById(id);
    }

    public void update(ObjectId id, NotificationStatus status, String adminEmail){
        AdminNotification notification = notificationRepository.findById(id);

        notification.setHandledBy(adminEmail);
        notification.setStatus(status);
        notificationRepository.update(notification);
    }
}
