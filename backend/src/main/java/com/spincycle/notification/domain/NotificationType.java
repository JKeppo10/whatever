package com.spincycle.notification.domain;

/** Design document UC6.1: the three notification texts. */
public enum NotificationType {
    RESERVATION_CONFIRMED("Reservation confirmed"),
    LAUNDRY_DONE("Your laundry is done"),
    MACHINE_FAULT("Machine fault: your reservation was cancelled and refunded");

    private final String message;

    NotificationType(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}