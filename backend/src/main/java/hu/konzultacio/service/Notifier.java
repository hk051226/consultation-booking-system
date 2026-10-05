package hu.konzultacio.service;

import hu.konzultacio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class Notifier {
    private static final Logger log = LoggerFactory.getLogger(Notifier.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm").withZone(ZoneId.of("Europe/Budapest"));

    private final JavaMailSender mail;
    private final UserRepository users;
    private final String from;

    public Notifier(JavaMailSender mail, UserRepository users, @Value("${app.mail-from}") String from) {
        this.mail = mail; this.users = users; this.from = from;
    }

    public static String fmt(Instant i) { return FMT.format(i); }

    /** Aszinkron, hibatűrő: az e-mail hibája sosem borítja a foglalást. */
    @Async
    public void send(Long userId, String subject, String text) {
        try {
            users.findById(userId).ifPresent(u -> {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(from);
                m.setTo(u.email);
                m.setSubject(subject);
                m.setText(text);
                mail.send(m);
            });
        } catch (Exception e) {
            log.warn("E-mail küldése sikertelen (user {}): {}", userId, e.getMessage());
        }
    }
}
