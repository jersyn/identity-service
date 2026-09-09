package com.identity.service.persistence.repository;

import com.identity.service.domain.TokenFamily;

import java.util.Optional;
import java.util.UUID;

public interface TokenFamilyRepository {

    Optional<TokenFamily> findById(UUID id);

    TokenFamily create(TokenFamily tokenFamily);
}
