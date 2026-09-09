package com.identity.service.persistence.repository;

import com.identity.service.domain.RefreshToken;
import com.identity.service.persistence.generated.tables.records.RefreshTokensRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

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
    public RefreshToken create(RefreshToken refreshToken) {
        RefreshTokensRecord record = dsl.newRecord(REFRESH_TOKENS);
        record.setTokenFamilyId(refreshToken.getTokenFamilyId());
        record.setStatus(refreshToken.getStatus());
        record.setExpiresAt(refreshToken.getExpiresAt());
        record.setUsedAt(refreshToken.getUsedAt());

        record.store();
        record.refresh();

        return toDomain(record);
    }

    private RefreshToken toDomain(RefreshTokensRecord record) {
        return new RefreshToken(
            record.getId(),
            record.getTokenFamilyId(),
            record.getStatus(),
            record.getExpiresAt(),
            record.getUsedAt()
        );
    }
}
