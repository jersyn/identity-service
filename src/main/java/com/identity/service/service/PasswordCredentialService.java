package com.identity.service.service;

import com.identity.service.domain.PasswordCredential;
import com.identity.service.persistence.repository.PasswordCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordCredentialService {

    private final PasswordCredentialRepository passwordCredentialRepository;

    public PasswordCredentialService(PasswordCredentialRepository passwordCredentialRepository) {
        this.passwordCredentialRepository = passwordCredentialRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PasswordCredential> findByUserId(UUID userId) {
        return passwordCredentialRepository.findByUserId(userId);
    }

    @Transactional
    public PasswordCredential createCredential(UUID userId, String passwordHash) {
        PasswordCredential credential = new PasswordCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordHash);
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        return passwordCredentialRepository.create(credential);
    }

    @Transactional
    public PasswordCredential updatePassword(UUID userId, String newPasswordHash) {
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Password credential not found for user: " + userId));

        credential.setPasswordHash(newPasswordHash);
        credential.setUpdatedAt(OffsetDateTime.now());

        return passwordCredentialRepository.update(credential);
    }
}
