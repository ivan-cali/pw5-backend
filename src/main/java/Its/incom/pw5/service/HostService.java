package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.enums.HostStatus;
import Its.incom.pw5.persistence.repository.HostRepository;
import Its.incom.pw5.rest.model.PasswordEditRequest;
import Its.incom.pw5.service.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@GlobalLog
@ApplicationScoped
public class HostService {
    @Inject
    HostRepository hostRepository;
    @Inject
    HashCalculator hashCalculator;

    @Inject
    UserService userService;


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
                throw new NotFoundException("User email not found");
            }

            Host newHost = new Host();
            newHost.setName(host.getName());
            newHost.setEmail(host.getEmail());
            newHost.setType(host.getType());
            newHost.setCreatedBy(user.getEmail());
            newHost.setHostStatus(HostStatus.PENDING);

            if (host.getType() == null || host.getType().toString().isEmpty() || host.getName() == null || host.getName().isEmpty() || host.getEmail() == null|| host.getEmail().isEmpty()) {
                throw new IllegalArgumentException("Form cannot have empty fields");
            }

            //check if host email already exists
            if (hostRepository.hostEmailExists(newHost.getEmail())) {
                throw new HostAlreadyExistsException("Host with email " + newHost.getEmail() + " already exists");
            }

            //check if host name already exists
            if (hostRepository.hostNameExists(newHost.getName())){
                throw new HostAlreadyExistsException("Host with name " + newHost.getName() + " already exists");
            }

            hostRepository.create(newHost);
        } catch (PersistenceException e) {
            throw new HostCreationException(e.getMessage());
        }
    }


    //delete host
    public void delete(Host host) throws HostNotFoundException, HostDeleteException {
        try {
            //check if host exists
            hostRepository.getById(host.getId());
            if (host == null) {
                throw new HostNotFoundException("Host not found");
            }
            hostRepository.delete(host);
        } catch (PersistenceException e) {
            throw new HostDeleteException(e.getMessage());
        }
    }


    public void changeHostPsw(Host host, PasswordEditRequest passwordEditRequest) throws HostNotFoundException, HostUpdateException {
        try {
            String newPsw = passwordEditRequest.getNewPsw();
            String oldPsw = passwordEditRequest.getOldPsw();

            //check string newPsw
            if (newPsw == null || newPsw.isEmpty()) {
                Map<String, Object> responseBody = Map.of(
                        "message", "New password not provided"
                );
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(responseBody).build());
            }

            //check string oldPsw
            if (oldPsw == null || oldPsw.isEmpty()) {
                Map<String, Object> responseBody = Map.of(
                        "message", "Old password not provided"
                );
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(responseBody).build());
            }

            //password hashing
            String newHashedPsw = hashCalculator.calculateHash(newPsw);
            String oldHashedPsw = hashCalculator.calculateHash(oldPsw);

            if (!Objects.equals(oldHashedPsw, host.getHashedPsw())) {
                Map<String, Object> responseBody = Map.of(
                        "message", "Old password is incorrect"
                );
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(responseBody).build());
            }

            if (Objects.equals(newHashedPsw, host.getHashedPsw())) {
                Map<String, Object> responseBody = Map.of(
                        "message", "New password cannot be the same as the old password"
                );
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(responseBody).build());
            }

            //update the old generated password with the new password
            host.setHashedPsw(newHashedPsw);
            hostRepository.updateHost(host);
        } catch (PersistenceException e) {
            throw new HostUpdateException(e.getMessage());
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

    public void rejectHostRequest(Host rejectedHost){
        rejectedHost.setHostStatus(HostStatus.REJECTED);
        hostRepository.updateHost(rejectedHost);
    }

    public Host getHostByEmail(String hostEmail) {
        return hostRepository.findByEmail(hostEmail);
    }

    public Host getHostByUserCreatorEmail(String userCreatorEmail){
        return hostRepository.getByUserCreatorEmail(userCreatorEmail);
    }

    public Host getHostById(String hostId) {
        return hostRepository.getById(new ObjectId(hostId));
    }

    public void updateEvents(Host host) {
        hostRepository.updateHost(host);
    }

    public boolean isPasswordMatching(String hashedPsw, String provvisoryPsw, String inputPassword) {
        if (hashedPsw == null || inputPassword == null) {
            return false;
        }

        // Hash the input password
        String hashedInputPsw = hashCalculator.calculateHash(inputPassword);

        // Compare the input password with both stored passwords
        return hashedInputPsw.equals(hashedPsw) || (provvisoryPsw != null && hashedInputPsw.equals(provvisoryPsw));
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
            throw new IllegalStateException("You must change your provisional password.");
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

}
