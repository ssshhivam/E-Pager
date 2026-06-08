package com.example.epager.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PushNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info(
                "Push notification to token={} title='{}' message='{}' deepLink={}",
                request.destination(),
                request.title(),
                request.message(),
                request.deepLink()
        );
        return NotificationResult.delivered("simulated-push-" + UUID.randomUUID());
    }
}
