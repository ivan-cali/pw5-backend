package Its.incom.pw5.rest.model;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.Ticket;

import java.util.List;

public class UserBookingResponse {
    private List<Event> bookedEvents;
    private List<Ticket> bookedTickets;

    // Getters and Setters
    public List<Event> getBookedEvents() {
        return bookedEvents;
    }

    public void setBookedEvents(List<Event> bookedEvents) {
        this.bookedEvents = bookedEvents;
    }

    public List<Ticket> getBookedTickets() {
        return bookedTickets;
    }

    public void setBookedTickets(List<Ticket> bookedTickets) {
        this.bookedTickets = bookedTickets;
    }
}