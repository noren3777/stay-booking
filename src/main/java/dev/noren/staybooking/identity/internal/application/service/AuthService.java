package dev.noren.staybooking.identity.internal.application.service;

import dev.noren.staybooking.identity.internal.application.exception.InvalidRegisterParameterException;
import dev.noren.staybooking.identity.internal.application.mapper.AuthMapper;
import dev.noren.staybooking.identity.internal.application.provider.AccessTokenIssuer;
import dev.noren.staybooking.identity.internal.application.provider.Actor;
import dev.noren.staybooking.identity.internal.application.provider.CredentialsAuthenticator;
import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.domain.normalization.EmailNormalizer;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.identity.internal.persistence.exception.UserPersistenceExceptionTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthMapper authMapper;
    private final CredentialsAuthenticator authenticator;
    private final UserPersistenceExceptionTranslator userPersistenceExceptionTranslator;

    public UserResult register(String email, String password, UserRole role) {
        if (role != null && !role.canRegister()) {
            throw new InvalidRegisterParameterException("Cannot register a user with this role");
        }

        String passwordHash = passwordEncoder.encode(password);

        try {
            User user = userRepository.save(
                    User.create(email, passwordHash, role)
            );
            return authMapper.toResult(user);
        } catch (DataIntegrityViolationException ex) {
            throw userPersistenceExceptionTranslator.translate(ex);
        }
    }

    public TokenResult login(String email, String password) {
        Actor actor = authenticator.authenticate(
                EmailNormalizer.normalize(email),
                password
        );

        String token = accessTokenIssuer.createAccessToken(actor);

        return new TokenResult(token, "Bearer");
    }

}
