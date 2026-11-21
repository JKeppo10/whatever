package com.spincycle.notification.repository;

import com.spincycle.notification.domain.Notification;
import com.spincycle.notification.domain.NotificationType;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** Temporary stand-in for the database. Data is lost on restart. */
@Repository
public class NotificationRepoTest implements NotificationRepository {

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Notification> byId = new ConcurrentHashMap<>();
    // Plays the role of the UNIQUE (user_id, source_event_id) constraint.
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    @Override
    public Optional<Notification> insertIfAbsent(long userId, NotificationType type, String message,
                                                 String sourceEventId) {
        if (!seen.add(userId + ":" + sourceEventId)) {
            return Optional.empty();
        }
        Notification n = new Notification(ids.incrementAndGet(), userId, type, message,
                sourceEventId, false, Instant.now());
        byId.put(n.id(), n);
        return Optional.of(n);
    }

    @Override
    public List<Notification> findByUserNewestFirst(long userId) {
        return byId.values().stream()
                .filter(n -> n.userId() == userId)
                .sorted(Comparator.comparingLong(Notification::id).reversed())
                .toList();
    }

    @Override
    public Optional<Notification> findByIdAndUser(long id, long userId) {
        return Optional.ofNullable(byId.get(id)).filter(n -> n.userId() == userId);
    }

    @Override
    public Notification save(Notification notification) {
        byId.put(notification.id(), notification);
        return notification;
    }
}