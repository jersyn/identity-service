package com.identity.service.persistence.repository;

import com.identity.service.domain.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findById(UUID id);

    Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

    List<RefreshToken> findAllByTokenFamilyId(UUID tokenFamilyId);

    RefreshToken create(RefreshToken refreshToken);

    int markAsUsed(UUID id);

    int revokeActiveByFamilyId(UUID tokenFamilyId);
}
