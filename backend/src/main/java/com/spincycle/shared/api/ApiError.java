package com.spincycle.shared.api;

import java.util.List;

//The single error body used by every endpoint (design doc; NFR08):
//{ "status", "code", "message", "path", "details": [{ "field", "message" }] }

public record ApiError(
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> details
) {
    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }
}