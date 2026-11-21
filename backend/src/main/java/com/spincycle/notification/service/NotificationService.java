package com.spincycle.notification.service;

import com.spincycle.notification.domain.Notification;
import com.spincycle.notification.domain.NotificationType;
import com.spincycle.notification.repository.NotificationRepository;
import com.spincycle.shared.api.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Business rules for UC6.1 and UC6.2. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** BR12: the same event never creates a second notification for the same user. Returns true if created. */
    public boolean createIfAbsent(long userId, NotificationType type, String sourceEventId) {
        var created = repository.insertIfAbsent(userId, type, type.message(), sourceEventId);
        if (created.isPresent()) {
            log.info("Notification created: id={} userId={} type={} eventId={}",
                    created.get().id(), userId, type, sourceEventId);
        } else {
            log.info("Duplicate event ignored: userId={} eventId={}", userId, sourceEventId);
        }
        return created.isPresent();
    }

    public List<Notification> list(long userId) {
        return repository.findByUserNewestFirst(userId);
    }

    /** UC6.2: another user's notification looks exactly like a missing one (404). */
    public Notification markRead(long userId, long notificationId) {
        Notification n = repository.findByIdAndUser(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found."));
        return repository.save(n.markRead());
    }
}