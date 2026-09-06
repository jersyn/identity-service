package com.identity.service.service;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.PasswordCredential;
import com.identity.service.domain.User;
import com.identity.service.persistence.repository.PasswordCredentialRepository;
import com.identity.service.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCredentialServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PasswordCredentialService passwordCredentialService;

    @Autowired
    private PasswordCredentialRepository passwordCredentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createCredentialShouldHashPassword() {
        User user = createUser();
        String rawPassword = "my-secure-password";

        PasswordCredential credential = passwordCredentialService.createCredential(user.getId(), rawPassword);

        assertThat(credential.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(credential.getPasswordHash()).startsWith("$argon2id$");
        assertThat(credential.getAlgorithm()).isEqualTo(PasswordCredential.ALGORITHM_ARGON2ID);
    }

    @Test
    void matchesShouldReturnTrueForCorrectPassword() {
        User user = createUser();
        String rawPassword = "correct-password";
        passwordCredentialService.createCredential(user.getId(), rawPassword);

        boolean result = passwordCredentialService.matches(user.getId(), rawPassword);

        assertThat(result).isTrue();
    }

    @Test
    void matchesShouldReturnFalseForWrongPassword() {
        User user = createUser();
        passwordCredentialService.createCredential(user.getId(), "correct-password");

        boolean result = passwordCredentialService.matches(user.getId(), "wrong-password");

        assertThat(result).isFalse();
    }

    @Test
    void matchesShouldReturnFalseForNonexistentUser() {
        boolean result = passwordCredentialService.matches(UUID.randomUUID(), "any-password");

        assertThat(result).isFalse();
    }

    @Test
    void updatePasswordShouldHashNewPassword() {
        User user = createUser();
        passwordCredentialService.createCredential(user.getId(), "old-password");

        passwordCredentialService.updatePassword(user.getId(), "new-password");

        assertThat(passwordCredentialService.matches(user.getId(), "old-password")).isFalse();
        assertThat(passwordCredentialService.matches(user.getId(), "new-password")).isTrue();
    }

    @Test
    void updatePasswordShouldPreserveAlgorithm() {
        User user = createUser();
        passwordCredentialService.createCredential(user.getId(), "old-password");

        passwordCredentialService.updatePassword(user.getId(), "new-password");

        PasswordCredential credential = passwordCredentialService.findByUserId(user.getId()).orElseThrow();
        assertThat(credential.getAlgorithm()).isEqualTo(PasswordCredential.ALGORITHM_ARGON2ID);
    }

    private User createUser() {
        User user = new User();
        user.setEmail("svc-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        return userRepository.create(user);
    }
}
