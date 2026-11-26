package dev.noren.staybooking.identity.internal.application.service;

import dev.noren.staybooking.identity.internal.application.exception.EmailAlreadyExistsException;
import dev.noren.staybooking.identity.internal.application.exception.InvalidRegisterParameterException;
import dev.noren.staybooking.identity.internal.application.mapper.AuthMapper;
import dev.noren.staybooking.identity.internal.application.provider.AccessTokenIssuer;
import dev.noren.staybooking.identity.internal.application.provider.CredentialsAuthenticator;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.identity.internal.persistence.exception.UserPersistenceExceptionTranslator;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private final static String EMAIL = "alice@email.com";
    private final static String PASSWORD = "Example-password-31";

    @Mock
    UserRepository users;

    @Mock
    AccessTokenIssuer tokens;

    @Mock
    CredentialsAuthenticator credentials;

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private final AuthMapper mapper = Mappers.getMapper(AuthMapper.class);

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                users,
                encoder,
                tokens,
                mapper,
                credentials,
                new UserPersistenceExceptionTranslator()
        );
    }

    @Test
    void registersUserWithNormalizedEmailAndEncodedPassword() {
        when(users.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        var result = service.register(
                " Alice@email.com ",
                PASSWORD,
                null
        );

        var saveUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(saveUser.capture());

        User user = saveUser.getValue();

        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getRole()).isEqualTo(UserRole.GUEST);
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, user.getPasswordHash())).isTrue();

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.role()).isEqualTo(UserRole.GUEST);
    }

    @Test
    void translatesDuplicateEmailConstraintIntoApplicationException() {
        var constraintViolation = new ConstraintViolationException(
                "duplicate email",
                new SQLException("duplicate email", "23505"),
                "uc_users_email"
        );

        when(users.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "insert failed",
                        constraintViolation
                ));

        assertThatThrownBy(() -> service.register(EMAIL, PASSWORD, null))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void rejectsPublicRegistrationWithAdminRole() {
        assertThatThrownBy(() ->
                service.register(EMAIL, PASSWORD, UserRole.ADMIN))
                .isInstanceOf(InvalidRegisterParameterException.class);

        verifyNoInteractions(users, tokens, credentials);
    }

    @Test
    void rejectsPasswordsThatCannotBeSafelyEncoded() {
        for (String password : new String[]{null, "short", "é".repeat(37)}) {
            assertThatThrownBy(() -> service.register(EMAIL, password, null))
                    .isInstanceOf(InvalidRegisterParameterException.class);
        }

        verifyNoInteractions(users, tokens, credentials);
    }

    @Test
    void invalidCredentialsDoNotIssueToken() {
        when(credentials.authenticate(EMAIL, PASSWORD))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                service.login(" Alice@email.com ", PASSWORD))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(tokens);
    }

    @Test
    void loginRejectsPasswordsBeyondBcryptLimitWithoutCallingAuthenticator() {
        assertThatThrownBy(() -> service.login(EMAIL, "é".repeat(37)))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(credentials, tokens);
    }

}
