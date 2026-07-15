package com.example.epager.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
    public List<NotificationResult> send(PushNotificationRequest request) {
        log.info(
                "Push notification title='{}' message='{}' deepLink={}",
                request.title(),
                request.message(),
                request.deepLink()
        );
        List<NotificationResult> results = new ArrayList<>();
        results.add(NotificationResult.delivered("simulated-push-" + UUID.randomUUID()));
        return results;
    }
}
