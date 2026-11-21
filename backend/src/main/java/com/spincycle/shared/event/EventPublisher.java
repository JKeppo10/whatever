package com.spincycle.shared.event;

/**
 * Publishes business events (design document section 6.2).
 * Milestone 2 uses {@link InProcessEventPublisher}; Milestone 3 adds a Kafka version
 * behind this same interface, so business code does not change.
 *
 * <p>Rules for handlers (CLAUDE.md, architecture rules):
 * <ul>
 *   <li>Listen with {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so the handler runs
 *       only after the publishing transaction commits, and a failing handler cannot roll it back.</li>
 *   <li>Handlers must be safe to run twice (idempotent).</li>
 * </ul>
 */
public interface EventPublisher {

    void publish(Object event);
}
