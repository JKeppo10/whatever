package com.spincycle.notification.domain;

import java.time.Instant;

/** One row of the future `notifications` table (design document section 7). */
public record Notification(
        long id,
        long userId,
        NotificationType type,
        String message,
        String sourceEventId,
        boolean isRead,
        Instant createdAt
) {
    public Notification markRead() {
        return new Notification(id, userId, type, message, sourceEventId, true, createdAt);
    }
}