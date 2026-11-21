package com.spincycle.notification.repository;

import com.spincycle.notification.domain.Notification;
import com.spincycle.notification.domain.NotificationType;
import java.util.List;
import java.util.Optional;

/**
 * Data access for notifications. Today the only implementation is in memory (no database yet).
 * TODO: replace with a JPA repository once F2 (database baseline) and migration V400 exist.
 */
public interface NotificationRepository {

    /** Inserts unless (userId, sourceEventId) already exists (BR12). Empty means "duplicate, nothing added". */
    Optional<Notification> insertIfAbsent(long userId, NotificationType type, String message, String sourceEventId);

    List<Notification> findByUserNewestFirst(long userId);

    /** Empty when the notification does not exist OR belongs to someone else (the API answers 404 for both). */
    Optional<Notification> findByIdAndUser(long id, long userId);

    Notification save(Notification notification);
}