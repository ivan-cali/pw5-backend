package Its.incom.pw5.persistence.model;

import java.util.ArrayList;

public class UserDetails {
    private ArrayList<Event> bookedEvents;
    private ArrayList<Event> archivedEvents;
    private ArrayList<Event> favouriteTopics;

    public ArrayList<Event> getBookedEvents() {
        return bookedEvents;
    }

    public void setBookedEvents(ArrayList<Event> bookedEvents) {
        this.bookedEvents = bookedEvents;
    }

    public ArrayList<Event> getArchivedEvents() {
        return archivedEvents;
    }

    public void setArchivedEvents(ArrayList<Event> archivedEvents) {
        this.archivedEvents = archivedEvents;
    }

    public ArrayList<Event> getFavouriteTopics() {
        return favouriteTopics;
    }

    public void setFavouriteTopics(ArrayList<Event> favouriteTopics) {
        this.favouriteTopics = favouriteTopics;
    }
}
