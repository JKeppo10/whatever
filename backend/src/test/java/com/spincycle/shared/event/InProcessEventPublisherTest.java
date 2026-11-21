package com.spincycle.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.spincycle.payment.event.PaymentCompleted;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(InProcessEventPublisherTest.Recorder.class)
class InProcessEventPublisherTest {

    @Autowired
    EventPublisher publisher;

    @Autowired
    Recorder recorder;

    @Test
    void publishedEventReachesListener() {
        PaymentCompleted event = new PaymentCompleted();

        publisher.publish(event);

        assertThat(recorder.received).containsExactly(event);
    }

    @TestConfiguration
    static class Recorder {
        final List<Object> received = new ArrayList<>();

        @EventListener
        void on(PaymentCompleted event) {
            received.add(event);
        }
    }
}
