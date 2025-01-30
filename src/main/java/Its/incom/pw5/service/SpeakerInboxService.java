package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import Its.incom.pw5.persistence.repository.UserRepository;
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
    UserRepository userRepository;

    @Inject
    EventRepository eventRepository;

    @Inject
    SessionService sessionService;

    public SpeakerInbox confirmRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.CONFIRMED);
    }

    public SpeakerInbox rejectRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.REJECTED);
    }

    private SpeakerInbox updateRequestStatusIfPending(ObjectId inboxId, SpeakerInboxStatus newStatus) {
        // Fetch the SpeakerInbox entry
        SpeakerInbox inbox = speakerInboxRepository.findByIdOptional(inboxId)
                .orElseThrow(() -> new WebApplicationException("SpeakerInbox not found", 404));

        // Ensure the current status is PENDING
        if (!inbox.getStatus().equals(SpeakerInboxStatus.PENDING)) {
            throw new WebApplicationException("Cannot update status. Current status is not PENDING.", 400);
        }

        // Update the status and persist the changes
        inbox.setStatus(newStatus);
        speakerInboxRepository.update(inbox);

        // Fetch the related event
        Event event = eventRepository.findByIdOptional(inbox.getEventId())
                .orElseThrow(() -> new WebApplicationException("Event not found", 404));

        // Remove the speaker from pendingSpeakerRequests
        if (event.getPendingSpeakerRequests() != null) {
            boolean removed = event.getPendingSpeakerRequests().removeIf(pendingSpeaker ->
                    pendingSpeaker.getEmail().equals(inbox.getSpeakerEmail()));
            if (removed) {
                System.out.println("Speaker " + inbox.getSpeakerEmail() + " removed from pending requests.");
            }
        }

        // If confirmed, add speaker details to the Event
        if (newStatus == SpeakerInboxStatus.CONFIRMED) {
            // Fetch the speaker user details
            User speaker = userRepository.getUserByEmail(inbox.getSpeakerEmail());
            if (speaker == null) {
                throw new WebApplicationException("User not found", 404);
            }

            // Check if the speaker is already in the list
            boolean isAlreadyAdded = event.getSpeakers().stream()
                    .anyMatch(existingSpeaker -> existingSpeaker.getEmail().equals(speaker.getEmail()));

            if (!isAlreadyAdded) {
                // Add the User object to the speakers list
                event.getSpeakers().add(speaker);
                System.out.println("Speaker " + speaker.getEmail() + " added to the event.");
            } else {
                System.out.println("Speaker " + speaker.getEmail() + " is already in the event.");
            }
        } else if (newStatus == SpeakerInboxStatus.REJECTED) {
            System.out.println("Speaker " + inbox.getSpeakerEmail() + " request was rejected.");
        }

        // Persist the updated event
        eventRepository.updateEvent(event);

        return inbox;
    }

    public List<SpeakerInbox> getRequestsForUser(String sessionCookie) {
        // Retrieve the user's email from the session cookie
        String userEmail = sessionService.findEmailBySessionCookie(sessionCookie);

        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("Invalid session or user email not found.");
        }

        // Fetch and return the SpeakerInbox requests for this email
        return speakerInboxRepository.findBySpeakerEmail(userEmail);
    }

    public void deleteRequest(ObjectId inboxId) {
        speakerInboxRepository.deleteRequest(inboxId);
    }
}
