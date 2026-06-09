package com.example.epager.notification;

import com.example.epager.notification.dto.NotificationLogResponse;
import com.example.epager.notification.dto.DeliveryTrackingRequest;
import com.example.epager.notification.dto.NotificationDeliveryEventResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationLogResponse> listNotifications() {
        return notificationService.findAll().stream()
                .map(NotificationLogResponse::from)
                .toList();
    }

    @PostMapping("/{notificationId}/received")
    public NotificationLogResponse markReceived(
            @PathVariable Long notificationId,
            @RequestBody(required = false) DeliveryTrackingRequest request
    ) {
        return NotificationLogResponse.from(notificationService.markReceived(
                notificationId,
                request == null ? null : request.clientInfo()
        ));
    }

    @PostMapping("/{notificationId}/seen")
    public NotificationLogResponse markSeen(
            @PathVariable Long notificationId,
            @RequestBody(required = false) DeliveryTrackingRequest request
    ) {
        return NotificationLogResponse.from(notificationService.markSeen(
                notificationId,
                request == null ? null : request.clientInfo()
        ));
    }

    @GetMapping("/{notificationId}/events")
    public List<NotificationDeliveryEventResponse> listDeliveryEvents(@PathVariable Long notificationId) {
        return notificationService.findDeliveryEvents(notificationId).stream()
                .map(NotificationDeliveryEventResponse::from)
                .toList();
    }
}
