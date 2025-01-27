package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.Type;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.ArrayList;

@MongoEntity(collection = "host")
public class Host {
    private ObjectId id;
    private Type type;
    private String name;
    private String description;
    private ArrayList<Event> pastEvents;
    private ArrayList<Event> programmedEvents;

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

    public ArrayList<Event> getPastEvents() {
        return pastEvents;
    }

    public void setPastEvents(ArrayList<Event> pastEvents) {
        this.pastEvents = pastEvents;
    }

    public ArrayList<Event> getProgrammedEvents() {
        return programmedEvents;
    }

    public void setProgrammedEvents(ArrayList<Event> programmedEvents) {
        this.programmedEvents = programmedEvents;
    }
}
