package dev.noren.staybooking.identity.internal.domain.entity;

import dev.noren.staybooking.bootstrap.entity.BaseEntity;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.domain.normalization.EmailNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    public static User create(String email, String passwordHash, UserRole role) {
        User user = new User();

        user.changeEmail(email);
        user.changePasswordHash(passwordHash);

        if (role == null) {
            user.role = UserRole.GUEST;
        } else {
            user.changeRole(role);
        }

        return user;
    }

    private void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidDomainArgumentException("email cannot be blank");
        }
        String normalized = EmailNormalizer.normalize(email);
        if (normalized.length() > 255) {
            throw new InvalidDomainArgumentException("email must not exceed 255 characters");
        }
        this.email = normalized;
    }

    private void changePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidDomainArgumentException("password hash cannot be blank");
        }
        this.passwordHash = passwordHash;
    }

    private void changeRole(UserRole role) {
        if (role == null) {
            throw new InvalidDomainArgumentException("role cannot be null");
        }
        this.role = role;
    }

}
