package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Event;
import Its.incom.pw5.persistence.model.VerificationToken;
import Its.incom.pw5.persistence.repository.VerificationTokenRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ApplicationScoped
public class MailService {
    private final ReactiveMailer mailer;
    private final VerificationTokenRepository verificationTokenRepository;

    private static final String BASE_URL = "http://localhost:8080";

    public MailService(ReactiveMailer mailer, VerificationTokenRepository verificationTokenRepository) {
        this.mailer = mailer;
        this.verificationTokenRepository = verificationTokenRepository;
    }


    public String generateToken(String email) {
        VerificationToken verificationToken = new VerificationToken();

        String token = UUID.randomUUID().toString();
        verificationToken.setToken(token);

        verificationToken.setEmail(email);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        verificationToken.setExpirationDate(tomorrow);

        verificationTokenRepository.createToken(verificationToken);

        return token;
    }


    public void sendVerificationMail(String email) {
        String token = generateToken(email);

        String verificationLink = BASE_URL + "/auth/confirm/" + token;

        Mail mail = Mail.withHtml(
                email,
                "Developer Varese Group - Conferma mail",
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Conferma Account</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background-color: #f4f4f4;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 20px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .email-header h1 {\n" +
                "            font-size: 24px;\n" +
                "            color: #333333;\n" +
                "        }\n" +
                "        .email-body {\n" +
                "            text-align: center;\n" +
                "            color: #555555;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .email-body p {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .btn-confirm {\n" +
                "            display: inline-block;\n" +
                "            margin-top: 20px;\n" +
                "            padding: 10px 20px;\n" +
                "            background-color: #4CAF50;\n" +
                "            color: #ffffff;\n" +
                "            text-decoration: none;\n" +
                "            font-size: 16px;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .btn-confirm:hover {\n" +
                "            background-color: #45a049;\n" +
                "        }\n" +
                "        .btn-confirm:active {\n" +
                "            color: #ffffff;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            font-size: 12px;\n" +
                "            color: #aaaaaa;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <h1>Conferma il tuo account</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Benvenuto in DVG</p>\n" +
                "            <p>Grazie per esserti registrato! Clicca sul link qui sotto per confermare il tuo account.</p>\n" +
                "            <a href=\"" + verificationLink + "\" class=\"btn-confirm\">Conferma account</a>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Se non hai richiesto la registrazione, ignora questa email.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>"
        );

        Uni<Void> send = mailer.send(mail);

        // controllo se la mail è stata inviata con successo
        send.subscribe().with(
                success -> System.out.println("Mail inviata"),
                failure -> System.out.println("Errore nell'invio della mail")
        );
    }

    public VerificationToken getVerificationToken(String token) {
        return verificationTokenRepository.findByToken(token);
    }

    public void sendBookingConfirmationMail(String email, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                email,
                "Developer Varese Group - Conferma prenotazione",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Conferma Prenotazione Evento</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background-color: #f4f4f4;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 20px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .email-header h1 {\n" +
                "            font-size: 24px;\n" +
                "            color: #333333;\n" +
                "        }\n" +
                "        .email-body {\n" +
                "            color: #555555;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .email-body p {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .event-details {\n" +
                "            margin: 20px 0;\n" +
                "            background-color: #f9f9f9;\n" +
                "            border: 1px solid #dddddd;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 15px;\n" +
                "        }\n" +
                "        .event-details p {\n" +
                "            margin: 5px 0;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            font-size: 12px;\n" +
                "            color: #aaaaaa;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <h1>Conferma della Prenotazione</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Siamo lieti di confermare la tua prenotazione per il seguente evento:</p>\n" +
                "            <div class=\"event-details\">\n" +
                "                <p><strong>Titolo Evento:</strong> " + event.getTitle() + "</p>\n" +
                "                <p><strong>Data:</strong> " + formattedDate + "</p>\n" +
                "                <p><strong>Luogo:</strong> <a href=\"https://www.google.com/maps/search/?api=1&query=" + event.getPlace() + "\">" + event.getPlace() + "</a></p>\n" +
                "            </div>\n" +
                "            <p>Non vediamo l'ora di vederti! Se hai domande, non esitare a contattarci.</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Se non hai effettuato questa prenotazione, ignora questa email.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>"
        );

        Uni<Void> send = mailer.send(mail);

        // controllo se la mail è stata inviata con successo
        send.subscribe().with(
                success -> System.out.println("Mail inviata"),
                failure -> System.out.println("Errore nell'invio della mail")
        );
    }

    public void sendBookingRevocationMail(String email, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                email,
                "Developer Varese Group - Annullamento prenotazione",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Annullamento Prenotazione Evento</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background-color: #f4f4f4;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 20px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .email-header h1 {\n" +
                "            font-size: 24px;\n" +
                "            color: #333333;\n" +
                "        }\n" +
                "        .email-body {\n" +
                "            color: #555555;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .email-body p {\n" +
                "            margin: 10px 0;\n" +
                "        }\n" +
                "        .event-details {\n" +
                "            margin: 20px 0;\n" +
                "            background-color: #f9f9f9;\n" +
                "            border: 1px solid #dddddd;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 15px;\n" +
                "        }\n" +
                "        .event-details p {\n" +
                "            margin: 5px 0;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            font-size: 12px;\n" +
                "            color: #aaaaaa;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <h1>Annullamento della Prenotazione</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Confermiamo l'annullamento della tua prenotazione per il seguente evento:</p>\n" +
                "            <div class=\"event-details\">\n" +
                "                <p><strong>Titolo Evento:</strong> " + event.getTitle() + "</p>\n" +
                "                <p><strong>Data:</strong> " + formattedDate + "</p>\n" +
                "            </div>\n" +
                "            <p>Siamo spiacenti di non poterti accogliere a questo evento. Se hai bisogno di assistenza o desideri ulteriori informazioni, non esitare a contattarci.</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Grazie per averci scelto. Speriamo di vederti a uno dei nostri prossimi eventi!</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>"
        );

        Uni<Void> send = mailer.send(mail);

        // controllo se la mail è stata inviata con successo
        send.subscribe().with(
                success -> System.out.println("Mail inviata"),
                failure -> System.out.println("Errore nell'invio della mail")
        );
    }
}
