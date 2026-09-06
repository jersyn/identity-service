package com.identity.service.persistence.repository;

import com.identity.service.domain.PasswordCredential;
import com.identity.service.persistence.generated.tables.records.PasswordCredentialsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.identity.service.persistence.generated.tables.PasswordCredentials.PASSWORD_CREDENTIALS;

@Repository
public class PasswordCredentialRepositoryImpl implements PasswordCredentialRepository {

    private final DSLContext dsl;

    public PasswordCredentialRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<PasswordCredential> findByUserId(UUID userId) {
        return dsl.selectFrom(PASSWORD_CREDENTIALS)
            .where(PASSWORD_CREDENTIALS.USER_ID.eq(userId))
            .fetchOptional(this::toDomain);
    }

    @Override
    public PasswordCredential create(PasswordCredential credential) {
        PasswordCredentialsRecord record = dsl.newRecord(PASSWORD_CREDENTIALS);
        record.setUserId(credential.getUserId());
        record.setPasswordHash(credential.getPasswordHash());
        record.setAlgorithm(credential.getAlgorithm());

        record.store();
        record.refresh();

        return toDomain(record);
    }

    @Override
    public PasswordCredential update(PasswordCredential credential) {
        dsl.update(PASSWORD_CREDENTIALS)
            .set(PASSWORD_CREDENTIALS.PASSWORD_HASH, credential.getPasswordHash())
            .set(PASSWORD_CREDENTIALS.ALGORITHM, credential.getAlgorithm())
            .set(PASSWORD_CREDENTIALS.UPDATED_AT, credential.getUpdatedAt())
            .where(PASSWORD_CREDENTIALS.USER_ID.eq(credential.getUserId()))
            .execute();

        return findByUserId(credential.getUserId())
            .orElseThrow(() -> new IllegalStateException(
                "Password credential not found after update for user: " + credential.getUserId()));
    }

    private PasswordCredential toDomain(PasswordCredentialsRecord record) {
        return new PasswordCredential(
            record.getUserId(),
            record.getPasswordHash(),
            record.getAlgorithm(),
            record.getUpdatedAt()
        );
    }
}
