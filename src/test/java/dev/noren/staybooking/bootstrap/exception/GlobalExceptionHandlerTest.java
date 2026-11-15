package dev.noren.staybooking.bootstrap.exception;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OptimisticLockController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void jpaOptimisticLockConflictReturnsPreconditionFailed() throws Exception {
        mvc.perform(get("/optimistic-lock"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.detail").value(
                        "Entity was modified by another request. Reload it and try again."));
    }

    @RestController
    private static class OptimisticLockController {
        @GetMapping("/optimistic-lock")
        void fail() {
            throw new OptimisticLockException("Concurrent update");
        }
    }
}
