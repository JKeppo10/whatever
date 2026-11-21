package com.spincycle.notification.controller;

import com.spincycle.identity.api.CurrentUser;
import com.spincycle.notification.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Design document 8.2. Signed-in users only (SecurityConfig: anyRequest().authenticated()).
 * The user id comes from the session through CurrentUser, never from the request.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return service.list(currentUser.id()).stream().map(NotificationResponse::from).toList();
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable long id) {
        return NotificationResponse.from(service.markRead(currentUser.id(), id));
    }
}