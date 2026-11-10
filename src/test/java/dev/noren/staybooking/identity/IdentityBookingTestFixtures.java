package dev.noren.staybooking.identity;

import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import jakarta.persistence.EntityManager;

import java.util.UUID;

public final class IdentityBookingTestFixtures {

    private IdentityBookingTestFixtures() {
    }

    public static TestUser persistUser(EntityManager entityManager, ActorRole role, String passwordHash) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        User user = User.create(email, passwordHash, UserRole.valueOf(role.name()));
        entityManager.persist(user);
        entityManager.flush();
        return new TestUser(user.getId(), user.getEmail());
    }

    public record TestUser(UUID id, String email) {
    }
}
