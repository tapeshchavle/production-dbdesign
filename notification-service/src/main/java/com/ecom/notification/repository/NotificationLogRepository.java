package com.ecom.notification.repository;

import com.ecom.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    List<NotificationLog> findByUserId(String userId);

    Optional<NotificationLog> findByIdempotencyKey(String idempotencyKey);

    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
