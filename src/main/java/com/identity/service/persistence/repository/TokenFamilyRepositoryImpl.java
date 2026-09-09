package com.identity.service.persistence.repository;

import com.identity.service.domain.TokenFamily;
import com.identity.service.persistence.generated.tables.records.TokenFamiliesRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.identity.service.persistence.generated.tables.TokenFamilies.TOKEN_FAMILIES;

@Repository
public class TokenFamilyRepositoryImpl implements TokenFamilyRepository {

    private final DSLContext dsl;

    public TokenFamilyRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<TokenFamily> findById(UUID id) {
        return dsl.selectFrom(TOKEN_FAMILIES)
            .where(TOKEN_FAMILIES.ID.eq(id))
            .fetchOptional(this::toDomain);
    }

    @Override
    public TokenFamily create(TokenFamily tokenFamily) {
        TokenFamiliesRecord record = dsl.newRecord(TOKEN_FAMILIES);
        record.setUserId(tokenFamily.getUserId());
        record.setStatus(tokenFamily.getStatus());
        record.setRevokedAt(tokenFamily.getRevokedAt());

        record.store();
        record.refresh();

        return toDomain(record);
    }

    private TokenFamily toDomain(TokenFamiliesRecord record) {
        return new TokenFamily(
            record.getId(),
            record.getUserId(),
            record.getStatus(),
            record.getCreatedAt(),
            record.getRevokedAt()
        );
    }
}
