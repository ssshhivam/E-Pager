package com.example.epager.notification.firebase;

import com.example.epager.notification.NotificationChannel;
import com.example.epager.notification.NotificationProvider;
import com.example.epager.notification.NotificationRequest;
import com.example.epager.notification.NotificationResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "epager.firebase", name = "enabled", havingValue = "true")
public class FirebasePushNotificationProvider implements NotificationProvider {

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushNotificationProvider(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        Message message = Message.builder()
                .setToken(request.destination())
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.message())
                        .build())
                .putData("notificationLogId", String.valueOf(request.notificationLogId()))
                .putData("incidentId", String.valueOf(request.incidentId()))
                .putData("userId", String.valueOf(request.userId()))
                .putData("severity", request.severity())
                .putData("deepLink", request.deepLink())
                .build();

        try {
            return NotificationResult.delivered(firebaseMessaging.send(message));
        } catch (FirebaseMessagingException exception) {
            return NotificationResult.failed(exception.getMessage());
        }
    }
}
