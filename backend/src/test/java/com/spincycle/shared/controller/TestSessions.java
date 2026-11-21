package com.spincycle.shared.controller;

import com.spincycle.identity.api.Role;
import com.spincycle.identity.service.SessionUser;
import java.util.List;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * AGAIN FOR LOCAL TESTING
 */
public final class TestSessions {

    private TestSessions() {
    }

    public static MockHttpSession signedIn(long userId, Role role) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                new SessionUser(userId, role), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(auth));
        return session;
    }
}