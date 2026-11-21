package com.spincycle.notification.event;

import com.spincycle.notification.domain.NotificationType;
import com.spincycle.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns business events into notifications (UC6.1). Follows the EventPublisher rules:
 * runs after the publisher's transaction commits, is safe to run twice, and never throws.
 */
@Component
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationService service;

    public NotificationEventHandler(NotificationService service) {
        this.service = service;
    }

    // fallbackExecution = true: also run when there is no transaction (true until the database exists).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentCompletedStub event) {
        try {
            service.createIfAbsent(event.userId(), NotificationType.RESERVATION_CONFIRMED, event.eventId());
        } catch (RuntimeException ex) {
            // BR12: a failed notification must never undo the payment. Log it; retry comes in M3.
            log.error("Could not create notification for eventId={}", event.eventId(), ex);
        }
    }
}