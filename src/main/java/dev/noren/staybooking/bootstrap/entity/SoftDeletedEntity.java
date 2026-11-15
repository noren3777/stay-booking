package dev.noren.staybooking.bootstrap.entity;

import dev.noren.staybooking.bootstrap.exception.EntityAlreadyDeletedException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class SoftDeletedEntity extends BaseEntity {

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    public void delete() {
        requireNotDeleted();
        deletedAt = Instant.now();
    }

    protected void requireNotDeleted() {
        if (isDeleted()) {
            throw new EntityAlreadyDeletedException();
        }
    }

    protected boolean isDeleted() {
        return deletedAt != null;
    }

}
