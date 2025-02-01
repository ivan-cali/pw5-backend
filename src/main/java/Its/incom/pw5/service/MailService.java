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

    // Angular frontend URL
    private static final String BASE_URL = "http://localhost:4200";

    public MailService(ReactiveMailer mailer, VerificationTokenRepository verificationTokenRepository) {
        this.mailer = mailer;
        this.verificationTokenRepository = verificationTokenRepository;
    }


    private String generateToken(String email) {
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

        String verificationLink = BASE_URL + "/auth/confirm-email/" + token;

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

    public void sendHostRequestRejectionEmail(String email) {

        Mail mail = Mail.withHtml(
                email,
                "CONFERMA ANNULLAMENTO CREAZIONE ACCOUNT AZIENDA/PARTNER",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Account Azienda/Partner Non Approvato</title>\n" +
                "<style>\n" +
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
                "        .email-footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            font-size: 12px;\n" +
                "            color: #aaaaaa;\n" +
                "        }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"email-container\">\n" +
                "<div class=\"email-header\">\n" +
                "<h1>Account Non Approvato</h1>\n" +
                "</div>\n" +
                "<div class=\"email-body\">\n" +
                "<p>Ti ringraziamo per aver richiesto la registrazione come Azienda/Partner sulla nostra piattaforma.</p>\n" +
                "<p>Purtroppo, dopo un'attenta revisione, non possiamo approvare la tua richiesta in questo momento.</p>\n" +
                "<p>Se ritieni che ci sia stato un errore o desideri maggiori informazioni, ti invitiamo a contattarci.</p>\n" +
                "</div>\n" +
                "<div class=\"email-footer\">\n" +
                "<p>Grazie per il tuo interesse nella nostra piattaforma.</p>\n" +
                "</div>\n" +
                "</div>\n" +
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

    public void sendHostRequestApprovalEmail(String email, String generatedPsw) {
        String verificationLink = BASE_URL + "/host/change-password";
        Mail mail = Mail.withHtml(
                email,
                "CONFERMA CREAZIONE ACCOUNT AZIENDA/PARTNER",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Conferma Creazione Account Azienda/Partner</title>\n" +
                "<style>\n" +
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
                "        .confirmation-button {\n" +
                "            display: block;\n" +
                "            width: 200px;\n" +
                "            margin: 20px auto;\n" +
                "            padding: 10px;\n" +
                "            background-color: #007bff;\n" +
                "            color: #ffffff;\n" +
                "            text-align: center;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .confirmation-button:hover {\n" +
                "            background-color: #0056b3;\n" +
                "        }\n" +
                "        .email-footer {\n" +
                "            text-align: center;\n" +
                "            margin-top: 20px;\n" +
                "            font-size: 12px;\n" +
                "            color: #aaaaaa;\n" +
                "        }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"email-container\">\n" +
                "<div class=\"email-header\">\n" +
                "<h1>Benvenuto!</h1>\n" +
                "</div>\n" +
                "<div class=\"email-body\">\n" +
                "<p>Ciao,</p>\n" +
                "<p>Grazie per aver creato un account come Azienda/Partner sulla nostra piattaforma.</p>\n" +
                "<p>Per completare la registrazione, ti preghiamo di confermare il tuo account. Ti abbiamo fornito una password provvisoria, ma per completare la verifica ed ottenere l'accesso dovrai cambiarla seguendo il link qui sotto:</p>\n" +
                "<p>Password provvisoria: " + generatedPsw + "</p>\n" +
                "<a href=\" " + verificationLink + "\" class=\"confirmation-button\">Conferma Account</a>\n" +
                "<p>Se non hai richiesto questa registrazione, ignora questa email.</p>\n" +
                "</div>\n" +
                "<div class=\"email-footer\">\n" +
                "<p>Grazie per aver scelto la nostra piattaforma!</p>\n" +
                "</div>\n" +
                "</div>\n" +
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

    public void sendBookingConfirmationMailToWaitingUser(String email, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                email,
                "Developer Varese Group - Aggiornamento - Conferma prenotazione",
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
                "            <p>Si è liberato un posto dall'evento a cui avevi richiesto di partecipare. Siamo lieti di confermare la tua prenotazione per il seguente evento:</p>\n" +
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


    public void sendEventDeletedMailToUser(String userEmail, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                userEmail,
                "Developer Varese Group - Evento Annullato",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Notifica Cancellazione Evento</title>\n" +
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
                "            <h1>Evento Annullato</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Ci dispiace informarti che l'evento a cui eri iscritto è stato annullato.</p>\n" +
                "            <div class=\"event-details\">\n" +
                "                <p><strong>Titolo Evento:</strong> " + event.getTitle() + "</p>\n" +
                "                <p><strong>Data:</strong> " + formattedDate + "</p>\n" +
                "                <p><strong>Luogo:</strong> <a href=\"https://www.google.com/maps/search/?api=1&query=" + event.getPlace() + "\">" + event.getPlace() + "</a></p>\n" +
                "            </div>\n" +
                "            <p>Ci scusiamo per l'inconveniente. Se hai domande, non esitare a contattarci.</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Grazie per la tua comprensione.</p>\n" +
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

    public void sendEventDeletedMailToWaitingUser(String waitingUserEmail, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                waitingUserEmail,
                "Developer Varese Group - Evento Annullato",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Notifica Cancellazione Evento - Lista d'Attesa</title>\n" +
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
                "            <h1>Evento Annullato</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Ti informiamo che l'evento per il quale eri in lista d'attesa è stato annullato.</p>\n" +
                "            <div class=\"event-details\">\n" +
                "                <p><strong>Titolo Evento:</strong> " + event.getTitle() + "</p>\n" +
                "                <p><strong>Data:</strong> " + formattedDate + "</p>\n" +
                "                <p><strong>Luogo:</strong> <a href=\"https://www.google.com/maps/search/?api=1&query=" + event.getPlace() + "\">" + event.getPlace() + "</a></p>\n" +
                "            </div>\n" +
                "            <p>Ci dispiace per l'inconveniente. Speriamo di vederti a un altro evento in futuro!</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Grazie per il tuo interesse nei nostri eventi.</p>\n" +
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

    public void sendEventDeletedMailToSpeaker(String speakerEmail, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'alle' HH:mm");
        String formattedDate = event.getStartDate().format(formatter);

        Mail mail = Mail.withHtml(
                speakerEmail,
                "Developer Varese Group - Evento Annullato",
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Notifica Cancellazione Evento - Speaker</title>\n" +
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
                "            <h1>Evento Annullato</h1>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\">\n" +
                "            <p>Ti informiamo con dispiacere che l'evento in cui eri programmato come speaker è stato annullato.</p>\n" +
                "            <div class=\"event-details\">\n" +
                "                <p><strong>Titolo Evento:</strong> " + event.getTitle() + "</p>\n" +
                "                <p><strong>Data:</strong> " + formattedDate + "</p>\n" +
                "                <p><strong>Luogo:</strong> <a href=\"https://www.google.com/maps/search/?api=1&query=" + event.getPlace() + "\">" + event.getPlace() + "</a></p>\n" +
                "            </div>\n" +
                "            <p>Ci scusiamo per l'inconveniente e speriamo di collaborare con te in un futuro evento.</p>\n" +
                "        </div>\n" +
                "        <div class=\"email-footer\">\n" +
                "            <p>Grazie per la tua disponibilità e professionalità.</p>\n" +
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
