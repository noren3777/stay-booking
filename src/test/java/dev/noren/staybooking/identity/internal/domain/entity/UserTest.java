package dev.noren.staybooking.identity.internal.domain.entity;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTest {

    private static final String HASH = "{bcrypt}already-encoded";

    @Test
    void normalizesEmailAndDefaultsRoleGuest() {
        User user = User.create(" Alice@mail.com ", HASH, null);

        assertThat(user.getEmail()).isEqualTo("alice@mail.com");
        assertThat(user.getRole()).isEqualTo(UserRole.GUEST);
    }

    @Test
    void preservesExplicitRoles() {
        User user = User.create("alice@mail.com", HASH, UserRole.HOST);

        assertThat(user.getRole()).isEqualTo(UserRole.HOST);
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> User.create("  ", HASH, null))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void rejectsEmailTooLongForStorageAfterNormalization() {
        assertThatThrownBy(() -> User.create("a".repeat(250) + "@mail.com  ", HASH, null))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void rejectsBlankPasswordHash() {
        assertThatThrownBy(() -> User.create("alice@mail.com", "", null))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

}
