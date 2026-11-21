package com.spincycle.notification.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spincycle.notification.domain.Notification;
import java.time.Instant;

/** Design document 8.2: id, type, message, isRead, createdAt. */
public record NotificationResponse(
        long id,
        String type,
        String message,
        @JsonProperty("isRead") boolean isRead,
        Instant createdAt
) {
    static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.id(), n.type().name(), n.message(), n.isRead(), n.createdAt());
    }
}