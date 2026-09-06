package com.identity.service.service;

import com.identity.service.domain.PasswordCredential;
import com.identity.service.persistence.repository.PasswordCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordCredentialService {

    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordCredentialService(PasswordCredentialRepository passwordCredentialRepository,
                                     PasswordEncoder passwordEncoder) {
        this.passwordCredentialRepository = passwordCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<PasswordCredential> findByUserId(UUID userId) {
        return passwordCredentialRepository.findByUserId(userId);
    }

    @Transactional
    public PasswordCredential createCredential(UUID userId, CharSequence rawPassword) {
        PasswordCredential credential = new PasswordCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        return passwordCredentialRepository.create(credential);
    }

    @Transactional
    public PasswordCredential updatePassword(UUID userId, CharSequence rawPassword) {
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Password credential not found for user: " + userId));

        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        credential.setUpdatedAt(OffsetDateTime.now());

        return passwordCredentialRepository.update(credential);
    }

    @Transactional(readOnly = true)
    public boolean matches(UUID userId, CharSequence rawPassword) {
        return passwordCredentialRepository.findByUserId(userId)
            .map(credential -> passwordEncoder.matches(rawPassword, credential.getPasswordHash()))
            .orElse(false);
    }
}
