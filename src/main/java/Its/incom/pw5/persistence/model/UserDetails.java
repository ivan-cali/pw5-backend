package Its.incom.pw5.persistence.model;

import java.util.List;

public class UserDetails {
    private List<Event> bookedEvents;
    private List<Event> archivedEvents;
    private List<Topic> favouriteTopics;

    public List<Event> getBookedEvents() {
        return bookedEvents;
    }

    public void setBookedEvents(List<Event> bookedEvents) {
        this.bookedEvents = bookedEvents;
    }

    public List<Event> getArchivedEvents() {
        return archivedEvents;
    }

    public void setArchivedEvents(List<Event> archivedEvents) {
        this.archivedEvents = archivedEvents;
    }

    public List<Topic> getFavouriteTopics() {
        return favouriteTopics;
    }

    public void setFavouriteTopics(List<Topic> favouriteTopics) {
        this.favouriteTopics = favouriteTopics;
    }
}
