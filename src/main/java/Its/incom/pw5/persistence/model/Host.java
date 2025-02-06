package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.HostStatus;
import Its.incom.pw5.persistence.model.enums.Type;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.util.List;

@MongoEntity(collection = "host")
public class Host {
    private ObjectId id;
    private Type type; // required
    private String name; // required
    private String email; // required
    private String hashedPsw;
    private String provisoryPsw;
    private String description; // ""
    private List<Event> pastEvents; // []
    private List<Event> programmedEvents; // []
    private String createdBy; // required

    private HostStatus hostStatus;

    public HostStatus getHostStatus() {
        return hostStatus;
    }

    public void setHostStatus(HostStatus hostStatus) {
        this.hostStatus = hostStatus;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPsw() {
        return hashedPsw;
    }

    public void setHashedPsw(String hashedPsw) {
        this.hashedPsw = hashedPsw;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Event> getPastEvents() {
        return pastEvents;
    }

    public void setPastEvents(List<Event> pastEvents) {
        this.pastEvents = pastEvents;
    }

    public List<Event> getProgrammedEvents() {
        return programmedEvents;
    }

    public void setProgrammedEvents(List<Event> programmedEvents) {
        this.programmedEvents = programmedEvents;
    }

    public String getProvisoryPsw() {
        return provisoryPsw;
    }

    public void setProvisoryPsw(String provisoryPsw) {
        this.provisoryPsw = provisoryPsw;
    }

}
