package com.identity.service.persistence.repository;

import com.identity.service.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findById(UUID id);

    RefreshToken create(RefreshToken refreshToken);
}
