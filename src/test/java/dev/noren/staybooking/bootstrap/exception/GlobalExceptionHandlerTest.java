package dev.noren.staybooking.bootstrap.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OptimisticLockController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .build();

    @Test
    void jpaOptimisticLockConflictReturnsPreconditionFailed() throws Exception {
        mvc.perform(get("/optimistic-lock"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("PRECONDITION_FAILED"))
                .andExpect(jsonPath("$.title").value("Precondition Failed"))
                .andExpect(jsonPath("$.path").value("/optimistic-lock"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(
                        "Entity was modified by another request. Reload it and try again."));
    }

    @Test
    void unexpectedFailureReturnsNeutralProblemWithoutInternalDetails() throws Exception {
        mvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/unexpected"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(content().string(not(containsString("secret SQL JWT stack trace"))));
    }

    @Test
    void malformedJsonAndBeanValidationHaveStableSafeDetails() throws Exception {
        mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request is invalid"))
                .andExpect(jsonPath("$.path").value("/validate"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void unsupportedMethodKeepsClientErrorStatusAndAllowHeader() throws Exception {
        mvc.perform(post("/unexpected"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeKeepsAcceptHeader() throws Exception {
        mvc.perform(post("/validate").contentType(MediaType.TEXT_PLAIN).content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", containsString("application/json")))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @RestController
    private static class OptimisticLockController {
        @GetMapping("/optimistic-lock")
        void fail() {
            throw new OptimisticLockException("Concurrent update");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret SQL JWT stack trace");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody @SuppressWarnings("unused") SampleRequest request) {
        }
    }

    private record SampleRequest(@NotBlank String name) {
    }
}
