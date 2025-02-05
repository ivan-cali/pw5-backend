package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.HostStatus;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.repository.HostRepository;
import Its.incom.pw5.rest.model.PasswordEditRequest;
import Its.incom.pw5.service.exception.*;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@GlobalLog
@ApplicationScoped
public class HostService {
    private final HostRepository hostRepository;
    private final UserService userService;
    private final HashCalculator hashCalculator;
    private final EventService eventService;

    public HostService(HostRepository hostRepository, UserService userService, HashCalculator hashCalculator, EventService eventService) {
        this.hostRepository = hostRepository;
        this.userService = userService;
        this.hashCalculator = hashCalculator;
        this.eventService = eventService;
    }

    //get all hosts
    public List<Host> getAll() throws HostNotFoundException {
        try {
            return hostRepository.getAll();
        } catch (PersistenceException e) {
            throw new HostNotFoundException("Hosts not found " + e.getMessage());
        }
    }

    //create host
    public void create(String userId, Host host) throws HostAlreadyExistsException, HostCreationException {
        try {
            User user = userService.getUserById(userId);
            if (user.getEmail() == null) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "User email not found"))
                        .build());
            }

            Host newHost = new Host();
            newHost.setName(host.getName());
            newHost.setEmail(host.getEmail());
            newHost.setType(host.getType());
            newHost.setCreatedBy(user.getEmail());
            newHost.setHostStatus(HostStatus.PENDING);

            if (host.getType() == null || host.getType().toString().isEmpty() || host.getName() == null || host.getName().isEmpty() || host.getEmail() == null || host.getEmail().isEmpty()) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Missing required fields"))
                        .build());
            }

            //check if host email already exists
            if (hostRepository.hostEmailExists(newHost.getEmail())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Host with email " + newHost.getEmail() + " already exists"))
                        .build());
            }

            //check if host name already exists
            if (hostRepository.hostNameExists(newHost.getName())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Host with name " + newHost.getName() + " already exists"))
                        .build());
            }

            hostRepository.create(newHost);
        } catch (PersistenceException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Host creation failed"))
                    .build());
        }
    }


    // Delete host and cascade delete events, tickets, etc.
    public void deleteHost(String userId, Host host) throws HostNotFoundException, HostDeleteException, UnauthorizedException {
        // Check if the user is an admin
        User user = userService.getUserById(userId);
        if (user == null || Role.ADMIN != user.getRole()) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Unauthorized to delete host"))
                    .build());
        }

        // Check if the host exists
        Host existingHost = hostRepository.getById(host.getId());
        if (existingHost == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Host not found"))
                    .build());
        }

        // Assuming existingHost is of type Host
        List<Event> events = eventService.getEventsByHostName(existingHost.getName());

// Cascade delete each event
        for (Event event : events) {
            eventService.deleteEvent(event.getId(), existingHost, true);
        }

        // Finally, delete the host
        hostRepository.deleteHost(existingHost);
        System.out.println("Successfully deleted host and all related events created by host: " + existingHost.getName());
    }


    public void changeHostPsw(Host host, PasswordEditRequest passwordEditRequest) throws HostNotFoundException, HostUpdateException {
        try {
            String newPsw = passwordEditRequest.getNewPsw();
            String oldPsw = passwordEditRequest.getOldPsw();

            //check string newPsw
            if (newPsw == null || newPsw.isEmpty()) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "New password not provided"))
                        .build());
            }

            //check string oldPsw
            if (oldPsw == null || oldPsw.isEmpty()) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Old password not provided"))
                        .build());
            }

            //password hashing
            String newHashedPsw = hashCalculator.calculateHash(newPsw);
            String oldHashedPsw = hashCalculator.calculateHash(oldPsw);

            if (!Objects.equals(oldHashedPsw, host.getHashedPsw())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Old password is incorrect"))
                        .build());
            }

            if (Objects.equals(newHashedPsw, host.getHashedPsw())) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "New password cannot be the same as the old password"))
                        .build());
            }

            //update the old generated password with the new password
            host.setHashedPsw(newHashedPsw);
            hostRepository.updateHost(host);
        } catch (PersistenceException e) {
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Host password update failed"))
                    .build());
        }
    }

    public Host findHostRequst(ObjectId id) {
        return hostRepository.getById(id);
    }

    public void update(Host newHost, String generatedPsw) {
        newHost.setHostStatus(HostStatus.APPROVED);
        String hashedPsw = hashCalculator.calculateHash(generatedPsw);
        newHost.setHashedPsw(hashedPsw);
        hostRepository.updateHost(newHost);
    }

    public void rejectHostRequest(Host rejectedHost) {
        rejectedHost.setHostStatus(HostStatus.REJECTED);
        hostRepository.updateHost(rejectedHost);
    }

    public Host getHostByEmail(String hostEmail) {
        return hostRepository.findByEmail(hostEmail);
    }

    public Host getHostByUserCreatorEmail(String userCreatorEmail) {
        return hostRepository.getByUserCreatorEmail(userCreatorEmail);
    }

    public Host getHostById(String hostId) {
        return hostRepository.getById(new ObjectId(hostId));
    }

    public void updateEvents(Host host) {
        hostRepository.updateHost(host);
    }


    public boolean isValidHostLogin(String email, String inputPassword) {
        Host validHost = getHostByEmail(email);

        if (validHost == null) {
            return false;  // No host found with the provided email
        }

        // Hash the input password
        String hashedInputPassword = hashPassword(inputPassword);

        // Check if the host is still using the provisional password
        if (validHost.getProvvisoryPsw() != null && hashedInputPassword.equals(validHost.getProvvisoryPsw())) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "You must change your provisional password before logging in."))
                    .build());
        }

        // Compare with the main hashed password
        if (!hashedInputPassword.equals(validHost.getHashedPsw())) {
            return false;  // Incorrect password
        }

        return true;  // Login is valid
    }

    public Host getValidHost(String email) {
        return getHostByEmail(email);
    }

    public String hashPassword(String password) {
        return hashCalculator.calculateHash(password);
    }

    public void updateDescription(Host host, String description) {
        host.setDescription(description);
        hostRepository.updateHost(host);
    }
}
