package dev.noren.staybooking.identity.internal.security.user;

import dev.noren.staybooking.identity.internal.application.provider.Actor;
import dev.noren.staybooking.identity.internal.application.provider.CredentialsAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppCredentialsAuthenticator implements CredentialsAuthenticator {

    private final AuthenticationManager authenticationManager;

    @Override
    public Actor authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        email,
                        password
                )
        );

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Unexpected authentication principal");
        }

        return new Actor(principal.id(), principal.role());
    }

}
