package com.spincycle.identity.api;

/**
 * The signed-in user, read from the server-side session, never from the request.
 * Only call it from endpoints that need sign-in; otherwise it answers 401.
 */
public interface CurrentUser {

    long id();

    Role role();
}
