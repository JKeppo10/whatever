package com.spincycle.shared.api;

import org.springframework.http.HttpStatus;

public class PaymentDeclinedException extends ApiException {
    public PaymentDeclinedException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message); // 422
    }
}