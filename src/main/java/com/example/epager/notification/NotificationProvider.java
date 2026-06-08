package com.example.epager.notification;

public interface NotificationProvider {

    NotificationChannel channel();

    NotificationResult send(NotificationRequest request);
}
