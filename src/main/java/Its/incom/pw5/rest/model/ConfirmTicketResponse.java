package Its.incom.pw5.rest.model;

import Its.incom.pw5.persistence.model.Ticket;

public class ConfirmTicketResponse {
    private final String ticketCode;
    private final String status;

    public ConfirmTicketResponse(Ticket ticket) {
        this.ticketCode = ticket.getTicketCode();
        this.status = ticket.getStatus().name();
    }

    // Getters

    public String getTicketCode() {
        return ticketCode;
    }

    public String getStatus() {
        return status;
    }
}