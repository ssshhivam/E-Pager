package com.example.epager.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findAllByOrderByCreatedAtDescIdDesc();

    List<NotificationLog> findByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId);
}
