package com.identity.service.persistence.repository;

import com.identity.service.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findById(UUID id);

    Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

    RefreshToken create(RefreshToken refreshToken);

    int markAsUsed(UUID id);
}
