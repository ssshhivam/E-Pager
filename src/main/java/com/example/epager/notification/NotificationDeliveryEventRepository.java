package com.example.epager.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryEventRepository extends JpaRepository<NotificationDeliveryEvent, Long> {

    List<NotificationDeliveryEvent> findByNotificationLogOrderByCreatedAtAsc(NotificationLog notificationLog);
}
