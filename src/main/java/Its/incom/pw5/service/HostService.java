package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.enums.Type;
import Its.incom.pw5.persistence.repository.HostRepository;
import Its.incom.pw5.service.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HostService {
    @Inject
    HostRepository hostRepository;

    //get all hosts
    public List<Host> getAll() throws HostNotFoundException{
        try {
            return hostRepository.getAll();
        } catch (PersistenceException e) {
            throw new HostNotFoundException("Hosts not found " + e.getMessage());
        }
    }

    //create host
    public Host create(Type hostType, String hostName, String hostEmail, String generatedPsw, String hostDescription) throws HostAlreadyExistsException, HostCreationException {
        try {
        //filling out the form
        if(!(hostType instanceof Type) || hostName == null || hostEmail == null || hostDescription == null){
            throw new IllegalArgumentException("From cannot have empty fields");
        }

        //check if host already exists
        if (hostRepository.hostNameExists(hostEmail)){
            throw new HostAlreadyExistsException("Host with email + " + hostEmail + " already exists");
        }

            return hostRepository.create(hostType, hostName, hostEmail, generatedPsw, hostDescription);
        } catch (PersistenceException e) {
            throw new HostCreationException(e.getMessage());
        }
    }



    //delete host
   /* public void delete(ObjectId sessionId,  ObjectId hostId) throws HostNotFoundException, HostDeleteException {
        try {
            //find if user exists, if he is verified and if he is logged in
            ObjectId userId;
            if (userId == null) {
                throw new SessionNotFoundException();
            }

            //check if user is admin
            if (!isAdmin(userId)){
                throw new UserNotAdminException();
            }

            //check if host exists
            Host host = hostRepository.getById(hostId);
            if (host == null){
                throw new HostNotFoundException("Host not found");
            }
                hostRepository.delete(host);
        } catch (PersistenceException e){
            throw new HostDeleteException(e.getMessage());
        }
    }
    */


    //update host
    public Host update(String sessionId, ObjectId hostId, Map<String, Object> updates) throws HostUpdateException{
        try {
            /*
            //find if user exists, if he is verified and if he is logged in
            ObjectId hostId;
            if (hostId == null) {
                throw new SessionNotFoundException();
            }*/

            //get host
            Host host = hostRepository.getById(hostId);

            return hostRepository.update(host, updates);
        } catch (IllegalArgumentException e){
            throw new HostUpdateException(e.getMessage());
        }
    }
}
