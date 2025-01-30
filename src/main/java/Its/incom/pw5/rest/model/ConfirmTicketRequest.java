package Its.incom.pw5.rest.model;

import jakarta.validation.constraints.NotBlank;

public class ConfirmTicketRequest {

    @NotBlank(message = "Ticket code must not be blank")
    private String ticketCode;

    // Getters and Setters

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }
}