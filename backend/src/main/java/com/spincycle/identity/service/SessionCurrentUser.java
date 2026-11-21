package com.spincycle.identity.service;

import com.spincycle.identity.api.CurrentUser;
import com.spincycle.identity.api.Role;
import com.spincycle.shared.api.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the user from the SecurityContext, which Spring loads from the HTTP session. */
@Component
public class SessionCurrentUser implements CurrentUser {

    @Override
    public long id() {
        return sessionUser().id();
    }

    @Override
    public Role role() {
        return sessionUser().role();
    }

    private SessionUser sessionUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SessionUser user) {
            return user;
        }
        throw new UnauthorizedException("UNAUTHORIZED", "Please sign in.");
    }
}
