package com.spincycle.shared.controller;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultHandler;

/**
 * Prints one API call (request and response) to the terminal in a short readable form.
 * Use: {@code mvc.perform(...).andDo(ApiPrinter.print("Alice lists her notifications"))}.
 */
public final class ApiPrinter {

    private ApiPrinter() {
    }

    public static ResultHandler print(String title) {
        return result -> {
            MockHttpServletRequest req = result.getRequest();
            MockHttpServletResponse res = result.getResponse();
            String query = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            System.out.println();
            System.out.println("=== " + title + " ===");
            System.out.println("  --> " + req.getMethod() + " " + req.getRequestURI() + query);
            String body = req.getContentAsString();
            if (body != null && !body.isBlank()) {
                System.out.println("      body: " + body);
            }
            System.out.println("  <-- " + res.getStatus() + "  (X-Request-Id: " + res.getHeader("X-Request-Id") + ")");
            String out = res.getContentAsString();
            if (out != null && !out.isBlank()) {
                System.out.println("      " + out);
            }
        };
    }

    /** For things that are not HTTP calls, such as publishing an event. */
    public static void step(String text) {
        System.out.println();
        System.out.println("*** " + text);
    }
}