package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.enums.Type;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HostRepository implements PanacheMongoRepository<Host> {

    //get all hosts
    public List<Host> getAll(){
        return listAll();
    }

    //create new host
    public Host create(Type type, String name, String email, String password, String description){
        Host host = new Host();
        host.setType(type);
        host.setName(name);
        host.setEmail(email);
        host.setHashedPsw(password);
        host.setDescription(description);
        persist(host);
        return host;
    }

    //delete host
    public void delete(Host host){
        delete(host);
    }

    //update host
    public Host update(Host host, Map<String, Object> updates){
        updates.forEach((key, value) -> {
            switch (key) {
                case "type":
                    if(value == null || !(value instanceof Type)) {
                        throw new IllegalArgumentException("Field type must have COMPANY or PARTNER value");
                    }
                    host.setType((Type) value);
                    break;

                case "name":
                    if(value == null || ((String) value).isEmpty()) {
                        throw new IllegalArgumentException("Field name cannot be empty");
                    }
                    host.setName((String) value);
                    break;

                case "description":
                    if(value == null || ((String) value).isEmpty()) {
                        throw new IllegalArgumentException("Field description cannot be empty");
                    }
                    host.setDescription((String) value);
                    break;

                default:
                    throw new IllegalArgumentException("Field " + key + " not valid");
            }
        });
        persistOrUpdate(host);
        return host;
    }

    //hostName already exist
    public boolean hostNameExists(String name){
        return find("name", name).firstResult() != null;
    }

    //get host by email
    public Host findByEmail(String email) {
        return find("email", email).firstResult();
    }

    //get host by id
    public Host getById (ObjectId id){
        return findById(id);
    }
}
