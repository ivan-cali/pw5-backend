package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.SpeakerInbox;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import Its.incom.pw5.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@GlobalLog
@ApplicationScoped
public class SpeakerInboxService {
    private final SpeakerInboxRepository speakerInboxRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    public SpeakerInboxService(SpeakerInboxRepository speakerInboxRepository, EventRepository eventRepository, UserRepository userRepository, SessionService sessionService) {
        this.speakerInboxRepository = speakerInboxRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    public SpeakerInbox confirmRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.CONFIRMED);
    }

    public SpeakerInbox rejectRequest(ObjectId inboxId) {
        return updateRequestStatusIfPending(inboxId, SpeakerInboxStatus.REJECTED);
    }

    private SpeakerInbox updateRequestStatusIfPending(ObjectId inboxId, SpeakerInboxStatus newStatus) {
        // Fetch the SpeakerInbox entry
        SpeakerInbox inbox = speakerInboxRepository.findByIdOptional(inboxId)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Speaker request not found."))
                        .build()));

        // Ensure the current status is PENDING
        if (!inbox.getStatus().equals(SpeakerInboxStatus.PENDING)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request is not pending."))
                    .build());
        }

        // Fetch the related event
        Event eventToConfirm = eventRepository.findByIdOptional(inbox.getEventId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Event not found for the request."))
                        .build()));

        // Check for overlapping events if the new status is CONFIRMED
        if (newStatus == SpeakerInboxStatus.CONFIRMED) {
            List<SpeakerInbox> confirmedRequests = speakerInboxRepository.findConfirmedRequestsByEmail(inbox.getSpeakerEmail());

            for (SpeakerInbox confirmedRequest : confirmedRequests) {
                Event confirmedEvent = eventRepository.findByIdOptional(confirmedRequest.getEventId())
                        .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Event not found for the request."))
                                .build()));

                // Check for date overlap
                if (datesOverlap(eventToConfirm.getStartDate(), eventToConfirm.getEndDate(),
                        confirmedEvent.getStartDate(), confirmedEvent.getEndDate())) {
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Event dates overlap with another confirmed event."))
                            .build());
                }
            }
        }

        // Update the status and persist the changes
        inbox.setStatus(newStatus);
        speakerInboxRepository.update(inbox);

        // Remove the speaker from pendingSpeakerRequests in the event
        if (eventToConfirm.getPendingSpeakerRequests() != null) {
            boolean removed = eventToConfirm.getPendingSpeakerRequests().removeIf(pendingSpeaker ->
                    pendingSpeaker.getEmail().equals(inbox.getSpeakerEmail()));
            if (removed) {
                System.out.println("Speaker " + inbox.getSpeakerEmail() + " removed from pending requests.");
            }
        }

        // If confirmed, add speaker details to the Event
        if (newStatus == SpeakerInboxStatus.CONFIRMED) {
            User speaker = userRepository.getUserByEmail(inbox.getSpeakerEmail());
            if (speaker == null) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Speaker not found."))
                        .build());
            }

            // Check if the speaker is already in the list
            boolean isAlreadyAdded = eventToConfirm.getSpeakers().stream()
                    .anyMatch(existingSpeaker -> existingSpeaker.getEmail().equals(speaker.getEmail()));

            // Add the User object to the speakers list
            if (!isAlreadyAdded) {
                eventToConfirm.getSpeakers().add(speaker);
                System.out.println("Speaker " + speaker.getEmail() + " added to the event.");
            } else {
                System.out.println("Speaker " + speaker.getEmail() + " is already in the event.");
            }
        } else if (newStatus == SpeakerInboxStatus.REJECTED) {
            System.out.println("Speaker " + inbox.getSpeakerEmail() + " request was rejected.");
        }

        // Persist the updated event
        eventRepository.updateEvent(eventToConfirm);

        return inbox;
    }

    private boolean datesOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return (start1.isBefore(end2) && end1.isAfter(start2));
    }

    public List<SpeakerInbox> getRequestsForUser(String sessionCookie, SpeakerInboxStatus requestStatus) {
        // Retrieve the user's email from the session cookie
        String userEmail = sessionService.findEmailBySessionCookie(sessionCookie);

        if (userEmail == null || userEmail.isBlank()) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid session cookie or user not found."))
                    .build());
        }

        if (requestStatus == null) {
            // Fetch and return the SpeakerInbox requests for this email
            return speakerInboxRepository.findBySpeakerEmail(userEmail);
        } else {
            // Fetch and return the SpeakerInbox requests filtered by the requests status for this email
            return speakerInboxRepository.getRequestsByStatus(requestStatus);
        }
    }

    public List<SpeakerInbox> getConfirmedRequestsForSpeaker(ObjectId speakerId) {
        User speaker = userRepository.findByIdOptional(speakerId)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Speaker not found."))
                        .build()));

        return speakerInboxRepository.findConfirmedRequestsByEmail(speaker.getEmail());
    }
}
