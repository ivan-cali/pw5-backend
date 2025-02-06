package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.*;
import Its.incom.pw5.persistence.model.enums.*;
import Its.incom.pw5.persistence.repository.EventRepository;
import Its.incom.pw5.persistence.repository.HostRepository;
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
import java.util.Map;
import java.util.Objects;

@GlobalLog
@ApplicationScoped
public class EventService {
    private final EventRepository eventRepository;
    private final TopicService topicService;
    private final UserService userService;
    private final SpeakerInboxRepository speakerInboxRepository;
    private final WaitingListService waitingListService;
    private final MailService mailService;
    private final TicketRepository ticketRepository;
    private final HostRepository hostRepository;

    public EventService(EventRepository eventRepository, TicketRepository ticketRepository, TopicService topicService, UserService userService, SpeakerInboxRepository speakerInboxRepository, WaitingListService waitingListService, MailService mailService, HostRepository hostRepository) {
        this.eventRepository = eventRepository;
        this.topicService = topicService;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.speakerInboxRepository = speakerInboxRepository;
        this.waitingListService = waitingListService;
        this.mailService = mailService;
        this.hostRepository = hostRepository;
    }
    public Event createEvent(Event event, String hostName) {
        return createEvent(event, hostName, null);
    }

    public Event createEvent(Event event, String hostName, ObjectId hostId) {
        // Validate event start date
        if (event.getStartDate() == null || event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event start date is required and cannot be in the past."))
                    .build());
        }

        // Validate event end date
        if (event.getEndDate() == null || event.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event end date is required and cannot be in the past."))
                    .build());
        }

        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event end date cannot be before the start date."))
                    .build());
        }

        // Validate event place
        if (event.getPlace() == null || event.getPlace().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event place is required."))
                    .build());
        }

        // Validate event title
        if (event.getTitle() == null || event.getTitle().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event title is required."))
                    .build());
        }

        // Validate speakers (at least one speaker is required)
        if (event.getPendingSpeakerRequests() == null || event.getPendingSpeakerRequests().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "At least one speaker is required."))
                    .build());
        }

        // Validate that at least one topic is provided
        validateTopics(event);

        // Set default status to PENDING if not provided
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.PENDING);
        }

        // Validate event description
        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            event.setDescription("");
        }

        // Validate event subscription
        if (event.getEventSubscription() == null || event.getEventSubscription().toString().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event subscription is required."))
                    .build());
        }

        // Validate max participants
        if (event.getMaxParticipants() < 0) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Max participants must be 0 or a positive number."))
                    .build());
        }

        // Create and initialize a new event object
        Event newEvent = initializeNewEvent(event, hostName);

        // Persist the Event to generate an ID
        eventRepository.addEvent(newEvent);

        // Pre-create tickets if the event has a maximum number of participants
        if (newEvent.getMaxParticipants() > 0) {
            createUnassignedTickets(newEvent);
        }

        // Process pending speaker requests
        processPendingSpeakerRequests(newEvent.getPendingSpeakerRequests(), newEvent.getId());

        event.setPendingSpeakerRequests(null);

        // Check if the creation is done by an admin
        if (!"Admin".equals(hostName)) {
            // Retrieve the host by its ObjectId (if hostId is a valid ObjectId string)
            if (!ObjectId.isValid(hostId.toHexString())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Invalid host ID format."))
                        .build());
            }
            Host host = hostRepository.getById(hostId);
            if (host == null) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Host not found."))
                        .build());
            }
            host.getProgrammedEvents().add(newEvent);
            hostRepository.update(host);
        }
        return newEvent;
    }

    private void createUnassignedTickets(Event event) {
        List<ObjectId> ticketIds = new ArrayList<>();
        for (int i = 0; i < event.getMaxParticipants(); i++) {
            Ticket ticket = new Ticket(null, event.getId(), TicketStatus.PENDING);
            ticketRepository.addTicket(ticket);
            ticketIds.add(ticket.getId());
        }
        event.setTicketIds(ticketIds);
        eventRepository.updateEvent(event);
    }

    public void updateEvent(ObjectId id, Event updatedEvent) {
        // Fetch the existing event
        Event existingEvent = getExistingEvent(id);

        // Validate event dates
        validateEventDates(existingEvent, updatedEvent);

        // Check if the event is confirmed and throw an exception if it is
        if (existingEvent.getStatus() == EventStatus.CONFIRMED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Cannot update a confirmed event."))
                    .build());
        }


        // Track changes to the max participants field
        boolean maxParticipantsChanged = updatedEvent.getMaxParticipants() > 0
                && updatedEvent.getMaxParticipants() != existingEvent.getMaxParticipants();

        // Update editable fields
        updateEditableFields(existingEvent, updatedEvent);

        // If maxParticipants has changed, delete and regenerate tickets
        if (maxParticipantsChanged) {
            // Clear the ticketIds list
            existingEvent.getTicketIds().clear();

            // Delete all existing tickets
            deleteUnassignedTickets(existingEvent);

            // Regenerate tickets with the new maxParticipants value
            createUnassignedTickets(existingEvent);
        }

        // Process pending speaker requests if any
        if (updatedEvent.getPendingSpeakerRequests() != null && !updatedEvent.getPendingSpeakerRequests().isEmpty()) {
            List<User> newPendingRequests = new ArrayList<>();
            for (User speakerRequest : updatedEvent.getPendingSpeakerRequests()) {
                if (speakerRequest.getEmail() != null) {
                    User speaker = userService.getUserByEmail(speakerRequest.getEmail());
                    if (speaker == null) {
                        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("message", "User " + speakerRequest.getEmail() + " does not exist."))
                                .build());
                    }

                    if (checkIfSpeakerRequestExists(speaker.getEmail(), existingEvent.getId())) {
                        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("message", "Speaker request already exists for: " + speaker.getEmail()))
                                .build());
                    }

                    addSpeakerToEvent(speaker, existingEvent.getId());
                    newPendingRequests.add(speakerRequest);
                }
            }
            existingEvent.setPendingSpeakerRequests(newPendingRequests);
        }

        // Persist the updated event
        eventRepository.updateEvent(existingEvent);
        // Update the event inside the host's programmed events, unless the host is "Admin"
        if (!"Admin".equals(existingEvent.getHost())) {
            // Query the host by its name instead of treating it as an ObjectId
            Host host = hostRepository.find("name", existingEvent.getHost()).firstResult();
            if (host != null) {
                for (int i = 0; i < host.getProgrammedEvents().size(); i++) {
                    if (host.getProgrammedEvents().get(i).getId().equals(existingEvent.getId())) {
                        host.getProgrammedEvents().set(i, existingEvent);
                        break;
                    }
                }
                hostRepository.update(host);
            }
        }
    }


    public boolean checkIfSpeakerRequestExists(String speakerEmail, ObjectId eventId) {
        return speakerInboxRepository.existsBySpeakerEmailAndEventId(speakerEmail, eventId);
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
                        .entity(Map.of("message", "Event not found."))
                        .build()));
    }

    private void validateTopics(Event event) {
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "At least one topic is required."))
                    .build());
        }

        List<String> finalTopics = new ArrayList<>();
        for (String topicName : event.getTopics()) {
            Topic topic = topicService.findOrCreateTopic(topicName);
            if (topic != null) {
                finalTopics.add(topic.getName());
            }
            event.setTopics(new ArrayList<>(finalTopics));
        }
        event.setTopics(finalTopics);
    }

    private void validateEventDates(Event existingEvent, Event updatedEvent) {
        // Check if the event has already occurred
        if (existingEvent.getStartDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Cannot update an event that has already occurred."))
                    .build());
        }
    }

    private void updateEditableFields(Event existingEvent, Event updatedEvent) {
        if (updatedEvent.getPlace() != null) {
            existingEvent.setPlace(updatedEvent.getPlace());
        }
        if (updatedEvent.getTitle() != null) {
            existingEvent.setTitle(updatedEvent.getTitle());
        }
        if (updatedEvent.getTopics() != null) {
            existingEvent.setTopics(updatedEvent.getTopics());
        }
        if (updatedEvent.getStartDate() != null) {
            existingEvent.setStartDate(updatedEvent.getStartDate());
        }
        if (updatedEvent.getEndDate() != null) {
            existingEvent.setEndDate(updatedEvent.getEndDate());
        }
        if (updatedEvent.getMaxParticipants() > 0) {
            existingEvent.setMaxParticipants(updatedEvent.getMaxParticipants());
        }
        if (updatedEvent.getEventSubscription() != null) {
            existingEvent.setEventSubscription(updatedEvent.getEventSubscription());
        }
        if (updatedEvent.getDescription() != null) {
            existingEvent.setDescription(updatedEvent.getDescription());
        }
    }

    private void addSpeakerToEvent(User speaker, ObjectId eventId) {
        // Fetch the full user object from the database
        User fullSpeaker = userService.getUserByEmail(speaker.getEmail());
        if (fullSpeaker == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "User " + speaker.getEmail() + " does not exist."))
                    .build());
        }

        // Validate speaker role
        if (fullSpeaker.getRole() == null || !fullSpeaker.getRole().equals(Role.SPEAKER)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "User " + fullSpeaker.getEmail() + " is not a speaker."))
                    .build());
        }

        // Check for duplicates in the SpeakerInbox
        boolean duplicateRequest = speakerInboxRepository.find(
                "speakerEmail = ?1 and eventId = ?2",
                fullSpeaker.getEmail(), eventId
        ).firstResultOptional().isPresent();

        if (duplicateRequest) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Speaker request already exists for: " + fullSpeaker.getEmail()))
                    .build());
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

                // Move the event from programmedEvents to pastEvents inside the host
                Host host = hostRepository.find("name", event.getHost()).firstResult();
                if (host != null) {
                    host.getProgrammedEvents().removeIf(programmedEvent -> programmedEvent.getId().equals(event.getId()));
                    host.getPastEvents().add(event);
                    hostRepository.update(host);
                }
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
        List<SpeakerInbox> relatedInboxes = speakerInboxRepository.getRequestsByEventId(eventId);

        if (!relatedInboxes.isEmpty()) {
            for (SpeakerInbox inbox : relatedInboxes) {
                speakerInboxRepository.deleteRequest(inbox.getId());
                System.out.println("Deleted SpeakerInbox entry for speaker: " + inbox.getSpeakerEmail());
            }
        } else {
            System.out.println("No SpeakerInbox entries found for event ID: " + eventId);
        }
    }

    public void checkAndBookEvent(ObjectId id, User user) {
        // Check if the event is provided
        if (id == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event body is required."))
                    .build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));

        // Check if the user is a speaker for the event
        if (existingEvent.getSpeakers().stream().anyMatch(speaker -> speaker.getEmail().equals(user.getEmail()))) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Speaker cannot book their own event."))
                    .build());
        }

        // Check if the user has already booked the event (compare ObjectId)
        boolean alreadyBooked = user.getUserDetails().getBookedEvents().stream()
                .anyMatch(bookedEvent -> bookedEvent.getId().equals(existingEvent.getId()));

        if (alreadyBooked) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "User has already booked this event."))
                    .build());
        }

        // Check if the event is CONFIRMED or ARCHIVED
        if (existingEvent.getStatus() != EventStatus.CONFIRMED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event is not confirmed."))
                    .build());
        }

        // Check if the event has already occurred
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event has already occurred."))
                    .build());
        }

        // Check if the event is full. If the event is full, the user will be put on a waiting list.
        if (existingEvent.getMaxParticipants() > 0 && existingEvent.getRegisteredParticipants() >= existingEvent.getMaxParticipants()) {
            // Check if the waiting list already exists
            waitingListService.checkAndCreateWaitingList(existingEvent.getId());

            // Get the waiting list
            WaitingList waitingList = waitingListService.getWaitingListByEventId(existingEvent.getId());

            // Check if the user is already on the waiting list
            if (waitingList.getWaitingUsers().contains(user.getEmail())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "User is already on the waiting list."))
                        .build());
            }

            // Add the user to the waiting list
            waitingListService.addUserToWaitingList(waitingList, user);

            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event is full. User added to waiting list."))
                    .build());
        }
        // If the event has a limited number of participants, assign an unassigned ticket
        Ticket assignedTicket;

        if (existingEvent.getMaxParticipants() > 0) {
            // Find an existing unassigned ticket for the event
            assignedTicket = ticketRepository.find("eventId = ?1 and status = ?2", existingEvent.getId(), TicketStatus.PENDING)
                    .firstResult();

            if (assignedTicket == null) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "No available tickets for this event."))
                        .build());
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
        userService.updateUser(user);
        updateRegisteredParticipants();
    }

    public void checkAndRevokeEvent(ObjectId eventId, User user) {
        // Check if the event is provided
        if (eventId == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event body is required."))
                    .build());
        }

        // Find the event in the database
        Event existingEvent = eventRepository.findByIdOptional(eventId)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));

        // Check if the user has already booked the event
        boolean isEventBooked = user.getUserDetails().getBookedEvents().stream()
                .anyMatch(bookedEvent -> bookedEvent.getId().equals(existingEvent.getId()));

        if (!isEventBooked) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "User has not booked this event."))
                    .build());
        }

        // Check if the event has already occurred or is ARCHIVED
        if (existingEvent.getEndDate().isBefore(LocalDateTime.now()) || existingEvent.getStatus() == EventStatus.ARCHIVED) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event has already occurred or is archived."))
                    .build());
        }

        // Check if the event starts within the next two weeks
        LocalDateTime twoWeeksFromNow = LocalDateTime.now().plusWeeks(2);
        if (existingEvent.getStartDate().isBefore(twoWeeksFromNow)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Event starts within the next two weeks."))
                    .build());
        }

        // Find the user's ticket for this event
        Ticket ticketToUpdate = ticketRepository.find("userId = ?1 and eventId = ?2", user.getId(), existingEvent.getId())
                .firstResult();

        // Check if the ticket exists
        if (ticketToUpdate == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Ticket not found."))
                    .build());
        }

        // Nullify the user ID to revoke the ticket's assignment
        ticketRepository.nullifyUserId(ticketToUpdate);
        Ticket existingTicket = ticketRepository.findByIdOptional(ticketToUpdate.getId())
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Ticket not found."))
                        .build()));

        // Refresh the ticket code
        ticketRepository.refreshTicketCode(existingTicket);

        // Remove the ticket ID from the event's ticketIds list
        existingEvent.getTicketIds().remove(ticketToUpdate.getId());

        // Remove the ticket from the user's booked tickets list
        user.getUserDetails().getBookedTickets().removeIf(ticket ->
                ticket.getId().equals(ticketToUpdate.getId()));

        // Remove the event from the user's booked events
        user.getUserDetails().getBookedEvents().removeIf(bookedEvent ->
                bookedEvent.getId().equals(existingEvent.getId()));

        // Remove user from the event's registered participants
        existingEvent.setRegisteredParticipants(existingEvent.getRegisteredParticipants() - 1);

        // Check if the event is full. If the event is full, check if it has a waiting list and add the first user to the event
        if (existingEvent.getMaxParticipants() > 0 && existingEvent.getRegisteredParticipants() < existingEvent.getMaxParticipants()) {
            // Check if the waiting list already exists
            WaitingList waitingList = waitingListService.getWaitingListByEventId(existingEvent.getId());

            // Check if the waiting list is not empty
            if (waitingList != null && !waitingList.getWaitingUsers().isEmpty()) {
                // Get the first user from the waiting list
                User firstUser = userService.getUserByEmail(waitingList.getWaitingUsers().get(0));

                // Remove the user from the waiting list
                waitingList.getWaitingUsers().removeIf(email ->
                        email.equals(firstUser.getEmail()));

                // Delete the waiting list if it's empty
                if (waitingList.getWaitingUsers().isEmpty()) {
                    waitingListService.deleteWaitingList(waitingList);
                }

                // Add the user to the event
                existingEvent.setRegisteredParticipants(existingEvent.getRegisteredParticipants() + 1);
                firstUser.getUserDetails().getBookedEvents().add(existingEvent);
                firstUser.getUserDetails().getBookedTickets().add(existingTicket);

                // Update the ticket with the user ID
                existingTicket.setUserId(firstUser.getId());
                ticketRepository.updateTicket(existingTicket);

                // Update the event's ticketIds list
                existingEvent.getTicketIds().add(existingTicket.getId());

                // Persist the updated waiting list
                waitingListService.updateWaitingList(waitingList);

                // Persist the updated event and user
                eventRepository.updateEvent(existingEvent);
                userService.updateUser(firstUser);

                // Email the user
                mailService.sendBookingConfirmationMailToWaitingUser(firstUser.getEmail(), existingEvent);
            }
        }

        // Persist the updated event and user
        eventRepository.updateEvent(existingEvent);
        userService.updateUser(user);
        updateRegisteredParticipants();
    }

    public Event getEventById(ObjectId eventId) {
        return eventRepository.findByIdOptional(eventId)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));
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
            if (event.getMaxParticipants() > 0) {
                registeredParticipants = (int) Math.min(ticketCount, event.getMaxParticipants());
            } else {
                // If no max participants limit, use the actual ticket count
                registeredParticipants = (int) ticketCount;
            }

            // Update the registered participants field with the ticket count
            event.setRegisteredParticipants(registeredParticipants);

            // Persist the updated event
            eventRepository.updateEvent(event);

            System.out.println("Updated event '" + event.getTitle() + "' with " + ticketCount + " registered participants.");
        }

        System.out.println("Scheduled task 'updateRegisteredParticipants' completed.");
    }

    public List<Event> getEventsByTopic(List<String> topics) {
        return eventRepository.getEventsByTopic(topics);
    }

    public List<Event> getEventsByDate(String date) {
        LocalDateTime localDate = LocalDateTime.parse(date);
        return eventRepository.getEventsByDate(localDate);
    }

    public List<Event> getEventsBySpeaker(List<String> speakersNames) {
        List<Event> events = new ArrayList<>();
        for (String s : speakersNames) {
            List<User> usersByFullName = userService.findUserByFullName(s);
            if (usersByFullName != null) {
                for (User u : usersByFullName) {
                    if (Role.SPEAKER.equals(u.getRole())) {
                        List<Event> eventsBySpeaker = eventRepository.getEventsBySpeakerMail(u.getEmail());
                        if (eventsBySpeaker != null) {
                            events.addAll(eventsBySpeaker);
                        }
                    }
                }
            }
        }
        return events;
    }

    public List<Event> getAllEvents() {
        return eventRepository.getAllEvents();
    }

    public void updateEventStatus(Event event) {
        eventRepository.updateEvent(event);
    }

    public void deleteEvent(ObjectId id, Host host, boolean isAdmin) {
        // Check if the event exists
        Event event = eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));

        // Check if the host is authorized to delete the event
        if (!Objects.equals(event.getHost(), host.getName())) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Host is not authorized to delete the event."))
                    .build());
        }
        // Remove from programmedEvents inside host (if not Admin)
        if (!"Admin".equals(event.getHost())) {
            // Remove the event from host's programmed events
            host.getProgrammedEvents().removeIf(programmedEvent -> programmedEvent.getId().equals(event.getId()));
            hostRepository.update(host);
        }

        // Remove the event from host both programmed (if CONFIRMED) and past (if ARCHIVED) events
        if (event.getStatus() == EventStatus.CONFIRMED) {
            host.getProgrammedEvents().removeIf(programmedEvent ->
                    programmedEvent.getId().equals(event.getId()));
        } else if (event.getStatus() == EventStatus.ARCHIVED) {
            host.getPastEvents().removeIf(pastEvent ->
                    pastEvent.getId().equals(event.getId()));
        } else {
            System.out.println("Event status is not CONFIRMED or ARCHIVED.");
        }

        // Remove the event from user both booked events (if CONFIRMED) and past events (if ARCHIVED)
        List<String> userEmailsToNotify = new ArrayList<>();
        if (event.getStatus() == EventStatus.CONFIRMED) {
            for (User user : userService.getAllUsers()) {
                user.getUserDetails().getBookedEvents().removeIf(bookedEvent ->
                        bookedEvent.getId().equals(event.getId()));
                userEmailsToNotify.add(user.getEmail());
                userService.updateUser(user);
            }
        } else if (event.getStatus() == EventStatus.ARCHIVED) {
            for (User user : userService.getAllUsers()) {
                user.getUserDetails().getArchivedEvents().removeIf(archivedEvent ->
                        archivedEvent.getId().equals(event.getId()));
                userEmailsToNotify.add(user.getEmail());
                userService.updateUser(user);
            }
        } else {
            System.out.println("Event status is not CONFIRMED or ARCHIVED.");
        }

        // Remove the ticket from user's booked tickets
        for (User user : userService.getAllUsers()) {
            user.getUserDetails().getBookedTickets().removeIf(ticket ->
                    ticket.getEventId().equals(event.getId()));
            userService.updateUser(user);
        }

        // Remove waiting list if it exists
        WaitingList waitingList = waitingListService.getWaitingListByEventId(event.getId());
        List<String> waitingUserEmailsToNotify = new ArrayList<>();
        if (waitingList != null) {
            waitingUserEmailsToNotify = waitingList.getWaitingUsers();
            waitingListService.deleteWaitingList(waitingList);
        }

        // Remove event tickets from the database
        if (event.getTicketIds() != null) {
            for (ObjectId ticketIds : event.getTicketIds()) {
                Ticket ticket = ticketRepository.findByIdOptional(ticketIds)
                        .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("message", "Ticket not found."))
                                .build()));
                ticketRepository.delete(ticket);
            }
        } else {
            System.out.println("No tickets found for event: " + event.getTitle());
        }

        // Remove speaker both inbox requests for the event and from the event
        List<String> speakerEmailsToNotify = new ArrayList<>();

        List<SpeakerInbox> relatedInboxes = speakerInboxRepository.getRequestsByEventId(event.getId());

        if (!relatedInboxes.isEmpty()) {
            for (SpeakerInbox inbox : relatedInboxes) {
                speakerEmailsToNotify.add(inbox.getSpeakerEmail());
            }
        }

        removeRelatedSpeakerInboxes(event.getId());

        // Remove the event from the database
        eventRepository.deleteEvent(event);

        // Notify users and speakers who booked the event that it has been deleted
        for (String userEmail : userEmailsToNotify) {
            mailService.sendEventDeletedMailToUser(userEmail, event);
        }

        for (String waitingUserEmail : waitingUserEmailsToNotify) {
            mailService.sendEventDeletedMailToWaitingUser(waitingUserEmail, event);
        }

        for (String speakerEmail : speakerEmailsToNotify) {
            mailService.sendEventDeletedMailToSpeaker(speakerEmail, event);
        }

    }

    public Event getEventByObjectId(ObjectId id) {
        return eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));
    }

    public void deleteEventAsAdmin(ObjectId id) {
        Event event = eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));

        eventRepository.deleteEvent(event);
    }

    public void updateEventAsAdmin(ObjectId id, Event updatedEvent) {
        Event existingEvent = eventRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Event not found."))
                        .build()));

        // If the event is created by the Admin, update the event in all fields
        if (Objects.equals(existingEvent.getHost(), "Admin")) {
            updateEvent(id, updatedEvent);
            return;
        } else {
            // Update editable fields
            updateEditableFields(existingEvent, updatedEvent);
        }

        // Persist the updated event
        eventRepository.updateEvent(existingEvent);
    }

    public List<Event> getEventsByHostName(String hostName) {
        if (hostName == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Host name cannot be null."))
                    .build());
        }
        // Query on the event's "host" field
        return eventRepository.find("host", hostName).list();
    }

    private static Event initializeNewEvent(Event event, String hostName) {
        Event newEvent = new Event();
        newEvent.setStartDate(event.getStartDate());
        newEvent.setEndDate(event.getEndDate());
        newEvent.setPlace(event.getPlace());
        newEvent.setTopics(new ArrayList<>(event.getTopics())); // Ensure list copy
        newEvent.setTitle(event.getTitle());
        newEvent.setStatus(event.getStatus());
        newEvent.setEventSubscription(event.getEventSubscription());
        newEvent.setMaxParticipants(event.getMaxParticipants());
        newEvent.setRegisteredParticipants(0);
        newEvent.setDescription(event.getDescription());
        newEvent.setSpeakers(new ArrayList<>());
        newEvent.setHost(hostName);
        newEvent.setTicketIds(new ArrayList<>());
        newEvent.setPendingSpeakerRequests(
                event.getPendingSpeakerRequests() != null ?
                        new ArrayList<>(event.getPendingSpeakerRequests()) : new ArrayList<>()
        );
        return newEvent;
    }

}
