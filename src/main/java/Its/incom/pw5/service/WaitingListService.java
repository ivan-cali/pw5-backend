package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.WaitingList;
import Its.incom.pw5.persistence.repository.WaitingListRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.ArrayList;

@GlobalLog
@ApplicationScoped
public class WaitingListService {
    private final WaitingListRepository waitingListRepository;

    public WaitingListService(WaitingListRepository waitingListRepository) {
        this.waitingListRepository = waitingListRepository;
    }

    public void addUserToWaitingList(WaitingList waitingList, User user) {
        waitingList.getWaitingUsers().add(user.getEmail());
        waitingListRepository.addUserToWaitingList(waitingList);
    }

    public void checkAndCreateWaitingList(ObjectId existingEventId) {
        // Check if the waiting list already exists
        WaitingList waitingList = waitingListRepository.getWaitingListByEventId(existingEventId);
        if (waitingList == null) {
            // Create a new waiting list
            waitingList = new WaitingList();
            waitingList.setEventId(existingEventId);
            waitingList.setWaitingUsers(new ArrayList<>());
            waitingListRepository.addWaitingList(waitingList);
        }
    }

    public WaitingList getWaitingListByEventId(ObjectId id) {
        return waitingListRepository.getWaitingListByEventId(id);
    }

    public void updateWaitingList(WaitingList waitingList) {
        waitingListRepository.updateWaitingList(waitingList);
    }

    public void deleteWaitingList(WaitingList waitingList) {
        waitingListRepository.deleteWaitingList(waitingList);
    }
}
