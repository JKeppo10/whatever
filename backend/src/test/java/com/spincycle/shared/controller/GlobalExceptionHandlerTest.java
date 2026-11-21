package com.spincycle.shared.controller;

import com.spincycle.shared.api.ConflictException;
import com.spincycle.shared.api.NotFoundException;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The security filters are on in this slice; run as a signed-in user so only error handling is tested.
@WithMockUser
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class, RequestIdFilter.class})
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    @RestController
    @Validated
    static class TestController {
        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("RESERVATION_CONFLICT",
                    "The machine is already reserved for that time.");
        }
        @GetMapping("/test/not-found")
        void notFound() { throw new NotFoundException("MACHINE_NOT_FOUND", "Machine not found."); }
        @GetMapping("/test/validation")
        void validation(@RequestParam @Min(1) int n) { }
        @GetMapping("/test/boom")
        void boom() { throw new IllegalStateException("do not leak this"); }
    }

    @Test
    void apiExceptionUsesSharedBody() throws Exception {
        mvc.perform(get("/test/conflict"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.status").value(409))
           .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"))
           .andExpect(jsonPath("$.path").value("/test/conflict"))
           .andExpect(jsonPath("$.details").isArray())
           .andExpect(header().exists(RequestIdFilter.HEADER));
    }

    @Test
    void validationReturns400WithFieldDetails() throws Exception {
        mvc.perform(get("/test/validation").param("n", "0"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
           .andExpect(jsonPath("$.details[0].message").exists());
    }

    @Test
    void unexpectedExceptionIsGeneric500() throws Exception {
        mvc.perform(get("/test/boom"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
           .andExpect(jsonPath("$.message", not(containsString("do not leak this"))));
    }

    @Test
    void requestIdIsEchoed() throws Exception {
        mvc.perform(get("/test/not-found").header(RequestIdFilter.HEADER, "test-id-1"))
           .andExpect(header().string(RequestIdFilter.HEADER, "test-id-1"));
    }
}