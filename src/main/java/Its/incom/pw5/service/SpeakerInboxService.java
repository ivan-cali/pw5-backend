package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class SpeakerInboxService {

    @Inject
    SpeakerInboxRepository speakerInboxRepository;

    @Inject
    SessionService sessionService;

    public SpeakerInbox confirmRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.CONFIRMED);
    }

    public SpeakerInbox rejectRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.REJECTED);
    }

    private SpeakerInbox updateRequestStatusIfPending(ObjectId inboxId, SpeakerInboxStatus newStatus) {
        // Fetch the SpeakerInbox entry by ID
        SpeakerInbox inbox = speakerInboxRepository.findByIdOptional(inboxId)
                .orElseThrow(() -> new WebApplicationException("SpeakerInbox not found", 404));

        // Ensure the current status is PENDING
        if (!inbox.getStatus().equals(SpeakerInboxStatus.PENDING)) {
            throw new WebApplicationException("Cannot update status. Current status is not PENDING.", 400);
        }

        // Update the status and persist the changes
        inbox.setStatus(newStatus);
        speakerInboxRepository.update(inbox);

        return inbox;
    }

    public List<SpeakerInbox> getRequestsForUser(String sessionCookie) {
        // Resolve user email from the session cookie
        String userEmail = sessionService.findEmailBySessionCookie(sessionCookie);

        if (userEmail == null) {
            throw new WebApplicationException("Invalid session cookie", 401);
        }

        // Debug: Log resolved user email
        System.out.println("Resolved user email: " + userEmail);

        // Use the repository method to fetch the requests
        List<SpeakerInbox> requests = speakerInboxRepository.findBySpeakerEmail(userEmail);

        // Debug: Log fetched requests
        System.out.println("Fetched requests for user: " + requests);

        return requests;
    }
}
