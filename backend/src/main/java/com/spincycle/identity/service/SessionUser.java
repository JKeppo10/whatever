package com.spincycle.identity.service;

import com.spincycle.identity.api.Role;
import java.io.Serializable;

/** What the session remembers about the signed-in user: no name, email or password hash. */
public record SessionUser(long id, Role role) implements Serializable {
}
