package com.example.epager.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "epager.firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class PushNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info(
                "Push notification id={} to token={} title='{}' message='{}' deepLink={}",
                request.notificationLogId(),
                request.destination(),
                request.title(),
                request.message(),
                request.deepLink()
        );
        return NotificationResult.delivered("simulated-push-" + UUID.randomUUID());
    }
}
