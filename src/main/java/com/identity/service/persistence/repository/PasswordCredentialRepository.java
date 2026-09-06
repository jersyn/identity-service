package com.identity.service.persistence.repository;

import com.identity.service.domain.PasswordCredential;

import java.util.Optional;
import java.util.UUID;

public interface PasswordCredentialRepository {

    Optional<PasswordCredential> findByUserId(UUID userId);

    PasswordCredential create(PasswordCredential credential);

    PasswordCredential update(PasswordCredential credential);
}
