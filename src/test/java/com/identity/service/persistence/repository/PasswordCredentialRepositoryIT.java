package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.PasswordCredential;
import com.identity.service.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCredentialRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordCredentialRepository passwordCredentialRepository;

    @Test
    void createShouldPersistCredential() {
        User user = createUser();
        PasswordCredential credential = new PasswordCredential();
        credential.setUserId(user.getId());
        credential.setPasswordHash("hashed-password-123");
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);

        PasswordCredential created = passwordCredentialRepository.create(credential);

        assertThat(created.getUserId()).isEqualTo(user.getId());
        assertThat(created.getPasswordHash()).isEqualTo("hashed-password-123");
        assertThat(created.getAlgorithm()).isEqualTo("ARGON2ID");
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUserIdShouldReturnExistingCredential() {
        User user = createUser();
        PasswordCredential credential = new PasswordCredential();
        credential.setUserId(user.getId());
        credential.setPasswordHash("hashed-password-123");
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        passwordCredentialRepository.create(credential);

        Optional<PasswordCredential> found = passwordCredentialRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(user.getId());
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-password-123");
    }

    @Test
    void findByUserIdShouldReturnEmptyForNonexistentUser() {
        Optional<PasswordCredential> found = passwordCredentialRepository.findByUserId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void updateShouldModifyPasswordHash() {
        User user = createUser();
        PasswordCredential credential = new PasswordCredential();
        credential.setUserId(user.getId());
        credential.setPasswordHash("old-hash");
        credential.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        PasswordCredential created = passwordCredentialRepository.create(credential);

        created.setPasswordHash("new-hash");
        created.setUpdatedAt(OffsetDateTime.now());
        PasswordCredential updated = passwordCredentialRepository.update(created);

        assertThat(updated.getUserId()).isEqualTo(user.getId());
        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void createShouldEnforceOneToOneRelationship() {
        User user = createUser();
        PasswordCredential credential1 = new PasswordCredential();
        credential1.setUserId(user.getId());
        credential1.setPasswordHash("first-hash");
        credential1.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);
        passwordCredentialRepository.create(credential1);

        PasswordCredential credential2 = new PasswordCredential();
        credential2.setUserId(user.getId());
        credential2.setPasswordHash("second-hash");
        credential2.setAlgorithm(PasswordCredential.ALGORITHM_ARGON2ID);

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> passwordCredentialRepository.create(credential2)
        );
    }

    private User createUser() {
        User user = new User();
        user.setStatus("ACTIVE");
        return userRepository.create(user);
    }
}
