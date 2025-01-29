package Its.incom.pw5.persistence.repository;

import Its.incom.pw5.persistence.model.Ticket;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TicketRepository implements PanacheMongoRepository<Ticket> {
    public void addTicket(Ticket ticket) {
        persist(ticket);
    }
}
