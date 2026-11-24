package dev.noren.staybooking.identity.internal.web.controller;

import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.application.service.UserService;
import dev.noren.staybooking.identity.internal.web.mapper.AuthWebMapper;
import dev.noren.staybooking.identity.internal.web.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthWebMapper mapper;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        UserResult result = userService.getCurrentUser();
        return mapper.toResponse(result);
    }

}
