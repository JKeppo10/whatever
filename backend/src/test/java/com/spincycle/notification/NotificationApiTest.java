package com.spincycle.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spincycle.identity.api.Role;
import com.spincycle.identity.config.SecurityConfig;
import com.spincycle.notification.event.PaymentCompletedStub;
import com.spincycle.shared.event.EventPublisher;
import com.spincycle.shared.controller.ApiPrinter;
import com.spincycle.shared.controller.TestSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Walks through the notification API with dummy users and events, printing every call.
 * Run: ./mvnw -Dtest=NotificationApiTest test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(NotificationApiTest.CsrfProbe.class)
class NotificationApiTest {

    @Autowired MockMvc mvc;
    @Autowired EventPublisher events;

    @Test
    void notificationFlowWorksThroughTheApi() throws Exception {
        MockHttpSession alice = TestSessions.signedIn(101, Role.CUSTOMER);
        MockHttpSession bob = TestSessions.signedIn(102, Role.CUSTOMER);

        // 1. A visitor is refused (401 in the shared error body).
        mvc.perform(get("/api/notifications"))
                .andDo(ApiPrinter.print("Visitor lists notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. Dummy events: two for Alice, one of them sent twice, and one for Bob.
        ApiPrinter.step("Publish PaymentCompleted evt-1 for Alice");
        events.publish(new PaymentCompletedStub("evt-1", 101, 5001));
        ApiPrinter.step("Publish evt-1 AGAIN (duplicate, must be ignored)");
        events.publish(new PaymentCompletedStub("evt-1", 101, 5001));
        ApiPrinter.step("Publish PaymentCompleted evt-2 for Alice");
        events.publish(new PaymentCompletedStub("evt-2", 101, 5002));
        ApiPrinter.step("Publish PaymentCompleted evt-3 for Bob");
        events.publish(new PaymentCompletedStub("evt-3", 102, 5003));

        // 3. Alice sees exactly two, newest first, both unread.
        String aliceBody = mvc.perform(get("/api/notifications").session(alice))
                .andDo(ApiPrinter.print("Alice lists her notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("RESERVATION_CONFIRMED"))
                .andExpect(jsonPath("$[0].message").value("Reservation confirmed"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andReturn().getResponse().getContentAsString();
        long newest = idAt(aliceBody, 0);
        long older = idAt(aliceBody, 1);
        assertThat(newest).isGreaterThan(older);

        // 4. Bob sees only his own.
        String bobBody = mvc.perform(get("/api/notifications").session(bob))
                .andDo(ApiPrinter.print("Bob lists his notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long bobsId = idAt(bobBody, 0);

        // 5. A state-changing call needs the CSRF header (403 without it).
        mvc.perform(patch("/api/notifications/{id}/read", newest).session(alice))
                .andDo(ApiPrinter.print("Alice marks read WITHOUT csrf token"))
                .andExpect(status().isForbidden());

        // 6. With the token it works and the flag flips.
        String csrf = mvc.perform(get("/api/test/csrf").session(alice))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(patch("/api/notifications/{id}/read", newest).session(alice)
                        .header(SecurityConfig.CSRF_HEADER, csrf))
                .andDo(ApiPrinter.print("Alice marks her newest notification read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        // 7. Alice cannot touch Bob's notification: 404, same as a missing one.
        mvc.perform(patch("/api/notifications/{id}/read", bobsId).session(alice)
                        .header(SecurityConfig.CSRF_HEADER, csrf))
                .andDo(ApiPrinter.print("Alice tries to mark BOB's notification read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));

        // 8. Bob's is still unread.
        mvc.perform(get("/api/notifications").session(bob))
                .andDo(ApiPrinter.print("Bob lists again (still unread)"))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    private static long idAt(String json, int index) {
        return ((Number) JsonPath.read(json, "$[" + index + "].id")).longValue();
    }

    /** Test-only endpoint that hands back the session's CSRF token (same trick as SecurityConfigTest). */
    @RestController
    static class CsrfProbe {
        @GetMapping("/api/test/csrf")
        String csrf(CsrfToken token) {
            return token.getToken();
        }
    }
}