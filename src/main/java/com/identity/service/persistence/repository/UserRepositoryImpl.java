package com.identity.service.persistence.repository;

import com.identity.service.domain.User;
import com.identity.service.persistence.generated.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.identity.service.persistence.generated.tables.Users.USERS;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final DSLContext dsl;

    public UserRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOptional(this::toDomain);
    }

    @Override
    public User create(User user) {
        UsersRecord record = dsl.newRecord(USERS);
        record.setStatus(user.getStatus());

        record.store();
        record.refresh();

        return toDomain(record);
    }

    @Override
    public User update(User user) {
        dsl.update(USERS)
            .set(USERS.STATUS, user.getStatus())
            .set(USERS.UPDATED_AT, user.getUpdatedAt())
            .where(USERS.ID.eq(user.getId()))
            .execute();

        return findById(user.getId())
            .orElseThrow(() -> new IllegalStateException(
                "User not found after update: " + user.getId()));
    }

    private User toDomain(UsersRecord record) {
        return new User(
            record.getId(),
            record.getStatus(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }
}
