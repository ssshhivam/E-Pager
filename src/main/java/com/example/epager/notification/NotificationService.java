package com.example.epager.notification;

import com.example.epager.incident.Incident;
import com.example.epager.user.AppUser;
import jakarta.persistence.EntityNotFoundException;
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
    private final NotificationDeliveryEventRepository notificationDeliveryEventRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final Map<NotificationChannel, NotificationProvider> providers;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            NotificationDeliveryEventRepository notificationDeliveryEventRepository,
            UserDeviceRepository userDeviceRepository,
            List<NotificationProvider> providers
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationDeliveryEventRepository = notificationDeliveryEventRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.providers = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::channel, Function.identity()));
    }

    @Transactional
    public void notifyUser(Incident incident, AppUser recipient) {
        List<UserDevice> devices = userDeviceRepository.findByUserAndActiveTrue(recipient);
        if (devices.isEmpty()) {
            NotificationLog log = createQueuedLog(incident, recipient, NotificationChannel.PUSH, recipient.getEmail());
            markProviderResult(log, NotificationResult.failed("No active push devices registered"));
            return;
        }

        NotificationProvider provider = providers.get(NotificationChannel.PUSH);
        if (provider == null) {
            devices.forEach(device -> {
                NotificationLog log = createQueuedLog(incident, recipient, NotificationChannel.PUSH, device.getPushToken());
                markProviderResult(log, NotificationResult.failed("No PUSH notification provider configured"));
            });
            return;
        }

        devices.forEach(device -> {
            NotificationLog log = createQueuedLog(incident, recipient, NotificationChannel.PUSH, device.getPushToken());
            NotificationRequest request = pushRequest(log, incident, recipient, device);
            NotificationResult result = provider.send(request);
            markProviderResult(log, result);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> findAll() {
        return notificationLogRepository.findAll();
    }

    @Transactional
    public NotificationLog markReceived(Long notificationLogId, String clientInfo) {
        NotificationLog log = findLog(notificationLogId);
        if (log.getStatus() != NotificationStatus.SEEN) {
            log.setStatus(NotificationStatus.RECEIVED);
        }
        log.setReceivedAt(LocalDateTime.now());
        NotificationLog savedLog = notificationLogRepository.save(log);
        updateDeviceLastSeen(log);
        recordDeliveryEvent(savedLog, NotificationStatus.RECEIVED, "Client reported notification received", clientInfo);
        return savedLog;
    }

    @Transactional
    public NotificationLog markSeen(Long notificationLogId, String clientInfo) {
        NotificationLog log = findLog(notificationLogId);
        LocalDateTime now = LocalDateTime.now();
        if (log.getReceivedAt() == null) {
            log.setReceivedAt(now);
        }
        log.setStatus(NotificationStatus.SEEN);
        log.setSeenAt(now);
        NotificationLog savedLog = notificationLogRepository.save(log);
        updateDeviceLastSeen(log);
        recordDeliveryEvent(savedLog, NotificationStatus.SEEN, "User opened notification", clientInfo);
        return savedLog;
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryEvent> findDeliveryEvents(Long notificationLogId) {
        return notificationDeliveryEventRepository.findByNotificationLogOrderByCreatedAtAsc(findLog(notificationLogId));
    }

    private NotificationRequest pushRequest(
            NotificationLog log,
            Incident incident,
            AppUser recipient,
            UserDevice device
    ) {
        return new NotificationRequest(
                log.getId(),
                incident.getId(),
                recipient.getId(),
                NotificationChannel.PUSH,
                device.getPushToken(),
                title(incident),
                message(incident),
                incident.getSeverity(),
                deepLink(incident, log)
        );
    }

    private NotificationLog findLog(Long notificationLogId) {
        return notificationLogRepository.findById(notificationLogId)
                .orElseThrow(() -> new EntityNotFoundException("Notification log not found: " + notificationLogId));
    }

    private NotificationLog createQueuedLog(
            Incident incident,
            AppUser recipient,
            NotificationChannel channel,
            String destination
    ) {
        NotificationLog log = new NotificationLog();
        log.setIncident(incident);
        log.setRecipient(recipient);
        log.setChannel(channel);
        log.setStatus(NotificationStatus.QUEUED);
        log.setDestination(destination);
        log.setTitle(title(incident));
        log.setMessage(message(incident));
        log.setDeepLink("/incidents/" + incident.getId());
        log.setDelivered(false);
        log.setCreatedAt(LocalDateTime.now());
        NotificationLog savedLog = notificationLogRepository.save(log);
        recordDeliveryEvent(savedLog, NotificationStatus.QUEUED, "Notification queued for provider", null);
        return savedLog;
    }

    private void markProviderResult(NotificationLog log, NotificationResult result) {
        log.setProviderMessageId(result.providerMessageId());
        log.setErrorMessage(result.errorMessage());
        log.setDelivered(result.delivered());
        if (result.delivered()) {
            log.setStatus(NotificationStatus.SENT);
            log.setSentAt(LocalDateTime.now());
            log.setDeepLink(deepLink(log.getIncident(), log));
            notificationLogRepository.save(log);
            recordDeliveryEvent(log, NotificationStatus.SENT, "Provider accepted notification", null);
        } else {
            log.setStatus(NotificationStatus.FAILED);
            log.setFailedAt(LocalDateTime.now());
            notificationLogRepository.save(log);
            recordDeliveryEvent(log, NotificationStatus.FAILED, result.errorMessage(), null);
        }
    }

    private String title(Incident incident) {
        return incident.getSeverity().toUpperCase() + " Alert: " + incident.getServiceName();
    }

    private String message(Incident incident) {
        return "Incident #" + incident.getId() + ": " + incident.getTitle();
    }

    private String deepLink(Incident incident, NotificationLog log) {
        return "/incidents/" + incident.getId() + "?notificationId=" + log.getId();
    }

    private void recordDeliveryEvent(
            NotificationLog log,
            NotificationStatus status,
            String detail,
            String clientInfo
    ) {
        NotificationDeliveryEvent event = new NotificationDeliveryEvent();
        event.setNotificationLog(log);
        event.setStatus(status);
        event.setDetail(detail);
        event.setClientInfo(clientInfo);
        event.setCreatedAt(LocalDateTime.now());
        notificationDeliveryEventRepository.save(event);
    }

    private void updateDeviceLastSeen(NotificationLog log) {
        if (log.getRecipient() == null || log.getDestination() == null) {
            return;
        }
        userDeviceRepository.findByUserAndActiveTrue(log.getRecipient()).stream()
                .filter(device -> log.getDestination().equals(device.getPushToken()))
                .findFirst()
                .ifPresent(device -> {
                    device.setLastSeenAt(LocalDateTime.now());
                    userDeviceRepository.save(device);
                });
    }
}
