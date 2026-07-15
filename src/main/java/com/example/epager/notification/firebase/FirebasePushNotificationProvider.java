package com.example.epager.notification.firebase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.epager.notification.NotificationChannel;
import com.example.epager.notification.NotificationProvider;
import com.example.epager.notification.NotificationResult;
import com.example.epager.notification.PushNotificationRequest;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

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
	public List<NotificationResult> send(PushNotificationRequest request) {
		MulticastMessage message = MulticastMessage.builder().addAllTokens(request.tokens())
				.setNotification(Notification.builder().setTitle(request.title()).setBody(request.message()).build())
				.putData("incidentId", String.valueOf(request.incidentId()))
				.putData("severity", request.severity())
				.putData("deepLink", request.deepLink())
				.build();
		try {
			BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
			List<NotificationResult> results = new ArrayList<>();
			for (SendResponse sendResponse : response.getResponses()) {
				if (sendResponse.isSuccessful()) {
					results.add(NotificationResult.delivered(sendResponse.getMessageId()));
				} else {
					FirebaseMessagingException exception = sendResponse.getException();
					results.add(
							NotificationResult.failed(exception != null ? exception.getMessage() : "Unknown error"));
				}
			}
			return results;
		} catch (FirebaseMessagingException exception) {
			return request.tokens().stream().map(token -> NotificationResult.failed(exception.getMessage())).toList();
		}
	}
}
