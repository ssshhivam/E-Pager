package com.example.epager.notification;

import com.example.epager.incident.Incident;
import com.example.epager.user.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional
    public void notifyUser(Incident incident, AppUser recipient) {
        String message = "Incident #" + incident.getId() + " [" + incident.getSeverity() + "] "
                + incident.getTitle() + " for service " + incident.getServiceName();

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setIncident(incident);
        notificationLog.setRecipient(recipient);
        notificationLog.setChannel(NotificationChannel.EMAIL);
        notificationLog.setDestination(recipient.getEmail());
        notificationLog.setMessage(message);
        notificationLog.setDelivered(true);
        notificationLog.setCreatedAt(LocalDateTime.now());
        notificationLogRepository.save(notificationLog);

        log.info("Notify {} via EMAIL <{}>: {}", recipient.getName(), recipient.getEmail(), message);
    }
}
