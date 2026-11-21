package com.spincycle.identity.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spincycle.identity.api.CurrentUser;
import com.spincycle.identity.api.Role;
import com.spincycle.identity.service.SessionUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigTest.ProbeController.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Test
    void visitorGets401InSharedErrorBody() throws Exception {
        mvc.perform(get("/api/probe/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/probe/me"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void visitorOnAdminEndpointGets401() throws Exception {
        mvc.perform(get("/api/admin/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerOnAdminEndpointGets403InSharedErrorBody() throws Exception {
        mvc.perform(get("/api/admin/probe").session(sessionFor(1, Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminOnAdminEndpointGets200() throws Exception {
        mvc.perform(get("/api/admin/probe").session(sessionFor(2, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void currentUserComesFromSessionNotRequest() throws Exception {
        mvc.perform(get("/api/probe/me").param("userId", "99").session(sessionFor(42, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(content().string("42 CUSTOMER"));
    }

    @Test
    void visitorPostWithoutCsrfTokenStillGets401() throws Exception {
        mvc.perform(post("/api/admin/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void signedInPostWithoutCsrfTokenGets403() throws Exception {
        mvc.perform(post("/api/probe").session(sessionFor(1, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedInPostWithWrongCsrfTokenGets403() throws Exception {
        mvc.perform(post("/api/probe").session(sessionFor(1, Role.CUSTOMER))
                        .header(SecurityConfig.CSRF_HEADER, "not-the-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedInPostWithSessionCsrfTokenInHeaderSucceeds() throws Exception {
        MockHttpSession session = sessionFor(1, Role.CUSTOMER);
        String token = mvc.perform(get("/api/probe/csrf").session(session))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/probe").session(session).header(SecurityConfig.CSRF_HEADER, token))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpointsDoNotNeedSignIn() throws Exception {
        // No controllers exist yet, so "allowed through" shows up as 404 rather than 401 or 403.
        mvc.perform(get("/api/machines/1/reviews")).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/register")).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login")).andExpect(status().isNotFound());
    }

    @Test
    void writingToMachinesStillNeedsSignIn() throws Exception {
        mvc.perform(post("/api/machines/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsReactDevServerWithCredentials() throws Exception {
        mvc.perform(options("/api/probe")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", SecurityConfig.CSRF_HEADER))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/probe")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    /** A session as AuthController (A2) will leave it after a successful sign-in. */
    static MockHttpSession sessionFor(long id, Role role) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                new SessionUser(id, role), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(auth));
        return session;
    }

    @RestController
    static class ProbeController {
        private final CurrentUser currentUser;

        ProbeController(CurrentUser currentUser) {
            this.currentUser = currentUser;
        }

        @GetMapping("/api/probe/me")
        String me() {
            return currentUser.id() + " " + currentUser.role();
        }

        @GetMapping("/api/probe/csrf")
        String csrf(CsrfToken token) {
            return token.getToken();
        }

        @PostMapping("/api/probe")
        String post() {
            return "ok";
        }

        @GetMapping("/api/admin/probe")
        String adminGet() {
            return "admin";
        }

        @PostMapping("/api/admin/probe")
        String adminPost() {
            return "admin";
        }
    }
}
