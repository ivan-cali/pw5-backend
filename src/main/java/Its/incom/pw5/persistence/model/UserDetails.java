package Its.incom.pw5.persistence.model;

import java.util.ArrayList;
import java.util.List;

public class UserDetails {
    private List<Event> bookedEvents = new ArrayList<>();
    private List<Event> archivedEvents = new ArrayList<>();
    private List<Ticket> bookedTickets = new ArrayList<>();
    private List<Topic> favouriteTopics = new ArrayList<>();

    // Getters and Setters

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

    public List<Ticket> getBookedTickets() {
        return bookedTickets;
    }

    public void setBookedTickets(List<Ticket> bookedTickets) {
        this.bookedTickets = bookedTickets;
    }

    public List<Topic> getFavouriteTopics() {
        return favouriteTopics;
    }

    public void setFavouriteTopics(List<Topic> favouriteTopics) {
        this.favouriteTopics = favouriteTopics;
    }
}
