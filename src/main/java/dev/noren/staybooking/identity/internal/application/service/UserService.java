package dev.noren.staybooking.identity.internal.application.service;

import dev.noren.staybooking.identity.internal.application.exception.UserNotFoundException;
import dev.noren.staybooking.identity.internal.application.provider.AuthProvider;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.identity.internal.application.provider.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthProvider authProvider;

    public UserResult getCurrentUser() {
        Actor actor = authProvider.currentActor();

        return userRepository.findCurrentUserById(actor.id())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }

}
