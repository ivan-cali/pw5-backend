package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.EventStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.SpeakerInboxStatus;
import Its.incom.pw5.persistence.model.enums.TicketStatus;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.SpeakerInboxRepository;
import Its.incom.pw5.persistence.repository.TicketRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EventService {
    private final EventRepository eventRepository;
    private final TopicService topicService;
    private final UserService userService;
    private final SpeakerInboxRepository speakerInboxRepository;
    private final TicketRepository ticketRepository;

    public EventService(EventRepository eventRepository,TicketRepository ticketRepository, TopicService topicService, UserService userService, SpeakerInboxRepository speakerInboxRepository) {
        this.eventRepository = eventRepository;
        this.topicService = topicService;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.speakerInboxRepository = speakerInboxRepository;
    }

    public Event createEvent(Event event) {
        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create an event with a past date.");
        }
        // Set default status to PENDING if not provided
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.PENDING);
        }

        // Validate that at least one topic is provided
        validateTopics(event);

        // Create and initialize a new event object
        Event newEvent = new Event();
        newEvent.setStartDate(event.getStartDate());
        newEvent.setEndDate(event.getEndDate());
        newEvent.setPlace(event.getPlace());
        newEvent.setTopics(new ArrayList<>(event.getTopics())); // Ensure list copy
        newEvent.setTitle(event.getTitle());
        newEvent.setStatus(event.getStatus());
        newEvent.setMaxPartecipants(event.getMaxPartecipants());
        newEvent.setRegisterdPartecipants(0);
        newEvent.setSpeakers(new ArrayList<>());
        newEvent.setHosts(new ArrayList<>()); // TODO: Implement hosts
        newEvent.setTicketIds(new ArrayList<>());
        newEvent.setPendingSpeakerRequests(
                event.getPendingSpeakerRequests() != null ? new ArrayList<>(event.getPendingSpeakerRequests()) : new ArrayList<>()
        );

        // Persist the Event to generate an ID
        eventRepository.addEvent(newEvent);
        if (newEvent.getId() == null) {
            throw new IllegalStateException("Failed to persist event, as the ID is null.");
        }

        // Pre-create tickets if the event has a maximum number of participants
        if (newEvent.getMaxPartecipants() > 0) {
            createUnassignedTickets(newEvent);
        }

        // Process pending speaker requests
        processPendingSpeakerRequests(newEvent.getPendingSpeakerRequests(), newEvent.getId());

        event.setPendingSpeakerRequests(null);

        return newEvent;
    }
    private void createUnassignedTickets(Event event) {
        List<ObjectId> ticketIds = new ArrayList<>();
        for (int i = 0; i < event.getMaxPartecipants(); i++) {
            Ticket ticket = new Ticket(null, event.getId(), TicketStatus.PENDING);
            ticketRepository.addTicket(ticket);
            ticketIds.add(ticket.getId());
        }
        event.setTicketIds(ticketIds);
        eventRepository.updateEvent(event);
    }

    public Event updateEvent(ObjectId id, Event updatedEvent, String speakerEmail) {
        // Fetch the existing event
        Event existingEvent = getExistingEvent(id);

        // Validate event dates
        validateEventDates(existingEvent, updatedEvent);

        // Update editable fields
        updateEditableFields(existingEvent, updatedEvent);

        // Process pending speaker requests properly
        if (updatedEvent.getPendingSpeakerRequests() != null && !updatedEvent.getPendingSpeakerRequests().isEmpty()) {
            List<User> newPendingRequests = new ArrayList<>();
            for (User speakerRequest : updatedEvent.getPendingSpeakerRequests()) {
                if (speakerRequest.getEmail() != null) {
                    User speaker = userService.getUserByEmail(speakerRequest.getEmail());
                    if (speaker == null) {
                        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                                .entity("User " + speakerRequest.getEmail() + " does not exist.").build());
                    }
                    addSpeakerToEvent(speaker, existingEvent.getId());
                    newPendingRequests.add(speakerRequest);
                }
            }
            // Ensure the event stores pending speaker requests
            existingEvent.setPendingSpeakerRequests(newPendingRequests);
        }

        // Handle optional speakerEmail query parameter
        if (speakerEmail != null && !speakerEmail.isBlank()) {
            User speaker = userService.getUserByEmail(speakerEmail);
            if (speaker == null) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("User " + speakerEmail + " does not exist.").build());
            }
            addSpeakerToEvent(speaker, existingEvent.getId());
        }

        // Persist updated event
        eventRepository.updateEvent(existingEvent);
        return existingEvent;
    }

    private void processPendingSpeakerRequests(List<User> pendingSpeakerRequests, ObjectId eventId) {
        if (pendingSpeakerRequests != null && !pendingSpeakerRequests.isEmpty()) {
            for (User speakerRequest : pendingSpeakerRequests) {
                try {
                    addSpeakerToEvent(speakerRequest, eventId);
                } catch (WebApplicationException e) {
                    // Log the error and propagate it to the client
                    System.err.println("Error processing speaker request for email: "
                                       + speakerRequest.getEmail() + ". Error: " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    private Event getExistingEvent(ObjectId id) {
        return eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));
    }

    private void validateTopics(Event event) {
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            throw new IllegalArgumentException("At least one topic is required.");
        }

        List<String> finalTopics = new ArrayList<>();
        for (String topicName : event.getTopics()) {
            Topic topic = topicService.findOrCreateTopic(topicName);
            if (topic != null) {
                finalTopics.add(topic.getName());
            }
        }
        event.setTopics(finalTopics);
    }

    private void validateEventDates(Event existingEvent, Event updatedEvent) {
        // Check if the event has already occurred
        if (existingEvent.getStartDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }
    }

    private void updateEditableFields(Event existingEvent, Event updatedEvent) {
        if (updatedEvent.getPlace() != null) {
            existingEvent.setPlace(updatedEvent.getPlace());
        }
        if (updatedEvent.getTopics() != null) {
            existingEvent.setTopics(updatedEvent.getTopics());
        }
        if (updatedEvent.getStartDate() != null) {
            existingEvent.setStartDate(updatedEvent.getStartDate());
        }
        if (updatedEvent.getMaxPartecipants() > 0) {
            existingEvent.setMaxPartecipants(updatedEvent.getMaxPartecipants());
        }
    }

    private void addSpeakerToEvent(User speaker, ObjectId eventId) {
        // Fetch the full user object from the database
        User fullSpeaker = userService.getUserByEmail(speaker.getEmail());
        if (fullSpeaker == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("User " + speaker.getEmail() + " does not exist.").build());
        }

        // Validate speaker role
        if (fullSpeaker.getRole() == null || !fullSpeaker.getRole().equals(Role.SPEAKER)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("User " + fullSpeaker.getEmail() + " is not a speaker.").build());
        }

        // Check for duplicates in the SpeakerInbox
        boolean duplicateRequest = speakerInboxRepository.find(
                "speakerEmail = ?1 and eventId = ?2",
                fullSpeaker.getEmail(), eventId
        ).firstResultOptional().isPresent();

        if (duplicateRequest) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Speaker request already exists for: " + fullSpeaker.getEmail()).build());
        }

        // Create a new SpeakerInbox entry with PENDING status
        SpeakerInbox inbox = new SpeakerInbox();
        inbox.setSpeakerEmail(fullSpeaker.getEmail());
        inbox.setEventId(eventId);
        inbox.setStatus(SpeakerInboxStatus.PENDING);
        speakerInboxRepository.addSpeakerInbox(inbox);

        // Log successful request
        System.out.println("Speaker request created for: " + fullSpeaker.getEmail());
    }
    public void onStartup(@Observes StartupEvent event) {
        System.out.println("Application started, running archivePastEvents now...");
        archivePastEvents();
    }
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void archivePastEvents() {
        System.out.println("Scheduled task 'archivePastEvents' started at: " + LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // Find all CONFIRMED events where the end date has already passed
        List<Event> eventsToArchive = eventRepository.find(
                "status = ?1 and endDate < ?2", EventStatus.CONFIRMED, now
        ).list();

        if (!eventsToArchive.isEmpty()) {
            for (Event event : eventsToArchive) {
                // Archive the event
                event.setStatus(EventStatus.ARCHIVED);
                eventRepository.updateEvent(event);
                System.out.println("Archived event: " + event.getTitle());

                // Remove all related SpeakerInbox entries
                removeRelatedSpeakerInboxes(event.getId());

                // Find all tickets for this event that have a userId assigned and are in PENDING status
                List<Ticket> ticketsToExpire = ticketRepository.find(
                        "eventId = ?1 and userId != null and status = ?2", event.getId(), TicketStatus.PENDING
                ).list();

                if (!ticketsToExpire.isEmpty()) {
                    for (Ticket ticket : ticketsToExpire) {
                        ticket.setStatus(TicketStatus.EXPIRED);
                        ticketRepository.updateTicket(ticket);
                        System.out.println("Expired ticket with ID: " + ticket.getId() + " for event: " + event.getTitle());
                    }
                } else {
                    System.out.println("No pending tickets to expire for event: " + event.getTitle());
                }

                deleteUnassignedTickets(event);
            }
        } else {
            System.out.println("No events to archive at this time.");
        }

        System.out.println("Scheduled task 'archivePastEvents' completed at: " + LocalDateTime.now());
    }

    private void deleteUnassignedTickets(Event event) {
        List<Ticket> unassignedTickets = ticketRepository.find(
                "{ 'eventId' : ?1, 'userId' : null }", event.getId()
        ).list();

        System.out.println("Found " + unassignedTickets.size() + " unassigned ticket(s) for event '" + event.getTitle() + "'");

        if (!unassignedTickets.isEmpty()) {
            List<ObjectId> deletedTicketIds = new ArrayList<>();
            for (Ticket ticket : unassignedTickets) {
                System.out.println("Attempting to delete ticket with ID: " + ticket.getId());
                // Delete the ticket using deleteEmptyTicket
                boolean deleted = ticketRepository.deleteEmptyTicket(ticket);
                if (deleted) {
                    System.out.println("Successfully deleted unassigned ticket with ID: " + ticket.getId());
                    deletedTicketIds.add(ticket.getId());
                } else {
                    System.err.println("Failed to delete unassigned ticket with ID: " + ticket.getId());
                }
            }

            // Remove the deleted ticket IDs from the event's ticketIds list
            if (event.getTicketIds() != null && !event.getTicketIds().isEmpty()) {
                System.out.println("Removing deleted ticket IDs from event's ticketIds list.");
                boolean removed = event.getTicketIds().removeAll(deletedTicketIds);
                if (removed) {
                    eventRepository.updateEvent(event);
                    System.out.println("Updated event '" + event.getTitle() + "' by removing " + deletedTicketIds.size() + " unassigned ticket(s).");
                } else {
                    System.err.println("No matching ticket IDs found in event's ticketIds list for event: " + event.getTitle());
                }
            }
        } else {
            System.out.println("No unassigned tickets to delete for event: " + event.getTitle());
        }
    }


    private void removeRelatedSpeakerInboxes(ObjectId eventId) {
        // Fetch all related SpeakerInbox entries for the event
        List<SpeakerInbox> relatedInboxes = speakerInboxRepository.find(
                "eventId", eventId
        ).list();

        if (!relatedInboxes.isEmpty()) {
            for (SpeakerInbox inbox : relatedInboxes) {
                speakerInboxRepository.delete(inbox);
                System.out.println("Deleted SpeakerInbox entry for speaker: " + inbox.getSpeakerEmail());
            }
        } else {
            System.out.println("No SpeakerInbox entries found for event ID: " + eventId);
        }

    }

    public void checkAndBookEvent(Event event, User user) {
        // Check if the event is provided
        if (event.getId() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(event.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));

        // Check if the user has already booked the event (compare ObjectId)
        boolean alreadyBooked = user.getUserDetails().getBookedEvents().stream()
                .anyMatch(bookedEvent -> bookedEvent.getId().equals(existingEvent.getId()));

        if (alreadyBooked) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("User has already booked this event.").build());
        }

        // Check if the event is CONFIRMED or ARCHIVED
        if (existingEvent.getStatus() != EventStatus.CONFIRMED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event is not confirmed.").build());
        }

        // Check if the event has already occurred
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }

        // Check if the event is full
        if (existingEvent.getMaxPartecipants() > 0 && existingEvent.getRegisterdPartecipants() >= existingEvent.getMaxPartecipants()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event is full.").build());
        }
        // If the event has a limited number of participants, assign an unassigned ticket
            Ticket assignedTicket;

            if (existingEvent.getMaxPartecipants() > 0) {
                // Find an existing unassigned ticket for the event
                assignedTicket = ticketRepository.find("eventId = ?1 and status = ?2", existingEvent.getId(), TicketStatus.PENDING)
                        .firstResult();

                if (assignedTicket == null) {
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity("No available tickets for this event.").build());
                }

                // Assign the ticket to the user
                assignedTicket.setUserId(user.getId());
                assignedTicket.setStatus(TicketStatus.PENDING);
                ticketRepository.updateTicket(assignedTicket);
            } else {
                // If no max participants, create a new ticket for the user
                assignedTicket = new Ticket(user.getId(), existingEvent.getId(), TicketStatus.PENDING);
                ticketRepository.addTicket(assignedTicket);
            }

        // 3. Add the ticket's ObjectId to the event's ticketIds list
        if (!existingEvent.getTicketIds().contains(assignedTicket.getId())) {
            existingEvent.getTicketIds().add(assignedTicket.getId());
        }

        // 4. Add the ticket to the user's booked tickets list
        user.getUserDetails().getBookedTickets().add(assignedTicket);

        // 5. Update event's user's booked events
        user.getUserDetails().getBookedEvents().add(existingEvent);

        // Persist the updated event and user
        eventRepository.updateEvent(existingEvent);
        userService.updateUserBookedEvents(user);
        updateRegisteredParticipants();
    }

    public void checkAndRevokeEvent(Event event, User user) {
        // Check if the event is provided
        if (event.getId() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event body is required.").build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(event.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));

        // Check if the event has already occurred
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Event has already occurred.").build());
        }

        // Check if the event starts within the next two weeks
        LocalDateTime twoWeeksFromNow = LocalDateTime.now().plusWeeks(2);
        if (existingEvent.getStartDate().isBefore(twoWeeksFromNow)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot revoke booking within two weeks of event start.").build());
        }

        // Find the user's ticket for this event
        Ticket ticketToUpdate = ticketRepository.find("userId = ?1 and eventId = ?2", user.getId(), existingEvent.getId())
                .firstResult();

        if (ticketToUpdate != null) {
            // Nullify the user ID to revoke the ticket's assignment
            ticketRepository.nullifyUserId(ticketToUpdate);
            Ticket existingTicket = ticketRepository.findByIdOptional(ticketToUpdate.getId())
                    .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                            .entity("Ticket not found with ID: " + ticketToUpdate.getId())
                            .build()));


            // Refresh the ticket code
            ticketRepository.refreshTicketCode(existingTicket);

            // Remove the ticket ID from the event's ticketIds list
            existingEvent.getTicketIds().remove(ticketToUpdate.getId());

            // Remove the ticket from the user's booked tickets list
            user.getUserDetails().getBookedTickets().removeIf(ticket -> ticket.getId().equals(ticketToUpdate.getId()));
        }

        // Remove the event from the user's booked events
        user.getUserDetails().getBookedEvents().removeIf(bookedEvent -> bookedEvent.getId().equals(existingEvent.getId()));

        // Remove the ticket from the user's booked tickets
        user.getUserDetails().getBookedTickets().removeIf(ticket ->
                ticket.getId().equals(ticketToUpdate.getId())
        );
        // Persist the updated event and user
        eventRepository.updateEvent(existingEvent);
        userService.updateUserBookedEvents(user);
        updateRegisteredParticipants();
    }


    public Event getEventById(Event eventId) {
        return eventRepository.findByIdOptional(eventId.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Event not found.").build()));
    }
    @Scheduled(cron = "0 0 0 * * ?")  // Runs every day at midnight
    public void updateRegisteredParticipants() {
        System.out.println("Scheduled task 'updateRegisteredParticipants' started.");

        // Fetch all events from the database
        List<Event> allEvents = eventRepository.findAll().list();

        for (Event event : allEvents) {
            // Count only tickets with a userId assigned for the event
            long ticketCount = ticketRepository.count("{ eventId: ?1, userId: { $ne: null } }", event.getId());

            // Determine the number of registered participants
            int registeredParticipants;
            if (event.getMaxPartecipants() > 0) {
                registeredParticipants = (int) Math.min(ticketCount, event.getMaxPartecipants());
            } else {
                // If no max participants limit, use the actual ticket count
                registeredParticipants = (int) ticketCount;
            }

            // Update the registered participants field with the ticket count
            event.setRegisterdPartecipants((int) ticketCount);

            // Persist the updated event
            eventRepository.updateEvent(event);

            System.out.println("Updated event '" + event.getTitle() + "' with " + ticketCount + " registered participants.");
        }

        System.out.println("Scheduled task 'updateRegisteredParticipants' completed.");
    }
}
