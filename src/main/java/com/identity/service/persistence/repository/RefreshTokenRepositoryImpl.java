package com.identity.service.persistence.repository;

import com.identity.service.domain.RefreshToken;
import com.identity.service.persistence.generated.tables.records.RefreshTokensRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.identity.service.persistence.generated.tables.RefreshTokens.REFRESH_TOKENS;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final DSLContext dsl;

    public RefreshTokenRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return dsl.selectFrom(REFRESH_TOKENS)
            .where(REFRESH_TOKENS.ID.eq(id))
            .fetchOptional(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(byte[] tokenHash) {
        return dsl.selectFrom(REFRESH_TOKENS)
            .where(REFRESH_TOKENS.TOKEN_HASH.eq(tokenHash))
            .fetchOptional(this::toDomain);
    }

    @Override
    public List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId) {
        return dsl.selectFrom(REFRESH_TOKENS)
            .where(REFRESH_TOKENS.TOKEN_FAMILY_ID.eq(tokenFamilyId))
            .fetch(this::toDomain);
    }

    @Override
    public RefreshToken create(RefreshToken refreshToken) {
        RefreshTokensRecord record = dsl.newRecord(REFRESH_TOKENS);
        record.setTokenFamilyId(refreshToken.getTokenFamilyId());
        record.setStatus(refreshToken.getStatus());
        record.setExpiresAt(refreshToken.getExpiresAt());
        record.setUsedAt(refreshToken.getUsedAt());
        record.setTokenHash(refreshToken.getTokenHash());

        record.store();
        record.refresh();

        return toDomain(record);
    }

    @Override
    public int markAsUsed(UUID id) {
        return dsl.update(REFRESH_TOKENS)
            .set(REFRESH_TOKENS.STATUS, "USED")
            .set(REFRESH_TOKENS.USED_AT, OffsetDateTime.now())
            .where(REFRESH_TOKENS.ID.eq(id))
            .and(REFRESH_TOKENS.STATUS.eq("ACTIVE"))
            .and(REFRESH_TOKENS.EXPIRES_AT.greaterThan(OffsetDateTime.now()))
            .execute();
    }

    @Override
    public int revokeActiveByFamilyId(UUID tokenFamilyId) {
        return dsl.update(REFRESH_TOKENS)
            .set(REFRESH_TOKENS.STATUS, "REVOKED")
            .where(REFRESH_TOKENS.TOKEN_FAMILY_ID.eq(tokenFamilyId))
            .and(REFRESH_TOKENS.STATUS.eq("ACTIVE"))
            .execute();
    }

    private RefreshToken toDomain(RefreshTokensRecord record) {
        return new RefreshToken(
            record.getId(),
            record.getTokenFamilyId(),
            record.getStatus(),
            record.getExpiresAt(),
            record.getUsedAt(),
            record.getTokenHash()
        );
    }
}
