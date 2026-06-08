package com.example.epager.notification;

import com.example.epager.incident.Incident;
import com.example.epager.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final Map<NotificationChannel, NotificationProvider> providers;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            UserDeviceRepository userDeviceRepository,
            List<NotificationProvider> providers
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.providers = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::channel, Function.identity()));
    }

    @Transactional
    public void notifyUser(Incident incident, AppUser recipient) {
        List<UserDevice> devices = userDeviceRepository.findByUserAndActiveTrue(recipient);
        if (devices.isEmpty()) {
            saveLog(incident, recipient, noDeviceRequest(incident, recipient), NotificationResult.failed("No active push devices registered"));
            return;
        }

        NotificationProvider provider = providers.get(NotificationChannel.PUSH);
        if (provider == null) {
            devices.forEach(device -> saveLog(
                    incident,
                    recipient,
                    pushRequest(incident, recipient, device),
                    NotificationResult.failed("No PUSH notification provider configured")
            ));
            return;
        }

        devices.forEach(device -> {
            NotificationRequest request = pushRequest(incident, recipient, device);
            NotificationResult result = provider.send(request);
            saveLog(incident, recipient, request, result);
        });
    }

    private NotificationRequest pushRequest(Incident incident, AppUser recipient, UserDevice device) {
        return new NotificationRequest(
                incident.getId(),
                recipient.getId(),
                NotificationChannel.PUSH,
                device.getPushToken(),
                title(incident),
                message(incident),
                incident.getSeverity(),
                deepLink(incident)
        );
    }

    private NotificationRequest noDeviceRequest(Incident incident, AppUser recipient) {
        return new NotificationRequest(
                incident.getId(),
                recipient.getId(),
                NotificationChannel.PUSH,
                recipient.getEmail(),
                title(incident),
                message(incident),
                incident.getSeverity(),
                deepLink(incident)
        );
    }

    private void saveLog(
            Incident incident,
            AppUser recipient,
            NotificationRequest request,
            NotificationResult result
    ) {
        NotificationLog log = new NotificationLog();
        log.setIncident(incident);
        log.setRecipient(recipient);
        log.setChannel(request.channel());
        log.setDestination(request.destination());
        log.setTitle(request.title());
        log.setMessage(request.message());
        log.setDeepLink(request.deepLink());
        log.setProviderMessageId(result.providerMessageId());
        log.setErrorMessage(result.errorMessage());
        log.setDelivered(result.delivered());
        log.setCreatedAt(LocalDateTime.now());
        notificationLogRepository.save(log);
    }

    private String title(Incident incident) {
        return incident.getSeverity().toUpperCase() + " Alert: " + incident.getServiceName();
    }

    private String message(Incident incident) {
        return "Incident #" + incident.getId() + ": " + incident.getTitle();
    }

    private String deepLink(Incident incident) {
        return "/incidents/" + incident.getId();
    }
}
