package dev.noren.staybooking.identity.internal.security.user;

import dev.noren.staybooking.identity.internal.application.provider.Actor;
import dev.noren.staybooking.identity.internal.application.provider.AuthProvider;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AppAuthProvider implements AuthProvider {

    @Override
    public Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication credentials not found");
        }

        String subject = jwtAuth.getToken().getSubject();
        if (subject == null) {
            throw new BadCredentialsException("Subject not found");
        }

        String role = jwtAuth.getToken().getClaimAsString("role");
        if (role == null) {
            throw new BadCredentialsException("Role not found");
        }

        try {
            return new Actor(
                    UUID.fromString(subject),
                    UserRole.valueOf(role)
            );
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid token actor", ex);
        }
    }

}
