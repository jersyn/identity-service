package com.identity.service.service;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.User;
import com.identity.service.persistence.repository.UserRepository;
import com.identity.service.security.HumanUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangePasswordServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ChangePasswordService changePasswordService;

    @Autowired
    private PasswordCredentialService passwordCredentialService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void changePasswordShouldUpdatePassword() {
        User user = createUser("ACTIVE");
        passwordCredentialService.createCredential(user.getId(), "old-password");
        HumanUserPrincipal principal = new HumanUserPrincipal(user);

        changePasswordService.changePassword(principal, "old-password", "new-password");

        assertThat(passwordCredentialService.matches(user.getId(), "old-password")).isFalse();
        assertThat(passwordCredentialService.matches(user.getId(), "new-password")).isTrue();
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = createUser("ACTIVE");
        passwordCredentialService.createCredential(user.getId(), "correct-password");
        HumanUserPrincipal principal = new HumanUserPrincipal(user);

        assertThatThrownBy(() -> changePasswordService.changePassword(principal, "wrong-password", "new-password"))
            .isInstanceOf(BadCredentialsException.class);

        assertThat(passwordCredentialService.matches(user.getId(), "correct-password")).isTrue();
    }

    @Test
    void changePasswordShouldRejectNonActiveUser() {
        User user = createUser("INACTIVE");
        passwordCredentialService.createCredential(user.getId(), "my-password");
        HumanUserPrincipal principal = new HumanUserPrincipal(user);

        assertThatThrownBy(() -> changePasswordService.changePassword(principal, "my-password", "new-password"))
            .isInstanceOf(BadCredentialsException.class);

        assertThat(passwordCredentialService.matches(user.getId(), "my-password")).isTrue();
    }

    @Test
    void changePasswordShouldRejectUserWithoutCredential() {
        User user = createUser("ACTIVE");
        HumanUserPrincipal principal = new HumanUserPrincipal(user);

        assertThatThrownBy(() -> changePasswordService.changePassword(principal, "any-password", "new-password"))
            .isInstanceOf(BadCredentialsException.class);
    }

    private User createUser(String status) {
        return userRepository.create(
            new User(null, status, "chg-" + UUID.randomUUID() + "@example.com", null, null));
    }
}
