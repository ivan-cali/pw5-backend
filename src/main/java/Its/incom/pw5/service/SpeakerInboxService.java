package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;

@ApplicationScoped
public class SpeakerInboxService {

    @Inject
    SpeakerInboxRepository speakerInboxRepository;

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
}
