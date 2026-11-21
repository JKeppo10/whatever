package com.spincycle.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Milestone 2 publisher: hands events to Spring's in-process event bus. */
@Component
public class InProcessEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public InProcessEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publish(Object event) {
        springPublisher.publishEvent(event);
    }
}
