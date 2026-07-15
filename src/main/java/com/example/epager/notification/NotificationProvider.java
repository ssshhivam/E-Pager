package com.example.epager.notification;

import java.util.List;

public interface NotificationProvider {

	NotificationChannel channel();

	List<NotificationResult> send(PushNotificationRequest request);

}
