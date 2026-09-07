package com.identity.service.security;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.User;
import com.identity.service.service.PasswordCredentialService;
import com.identity.service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordAuthenticationProviderIT extends AbstractIntegrationTest {

    @Autowired
    private PasswordAuthenticationProvider passwordAuthenticationProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordCredentialService passwordCredentialService;

    @Test
    void authenticateShouldReturnAuthenticatedTokenForValidCredentials() {
        User user = createUser("ACTIVE");
        passwordCredentialService.createCredential(user.getId(), "correct-password");

        PasswordAuthentication auth = PasswordAuthentication.ofCredentials(user.getEmail(), "correct-password");
        Authentication result = passwordAuthenticationProvider.authenticate(auth);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(HumanUserPrincipal.class);
        HumanUserPrincipal principal = (HumanUserPrincipal) result.getPrincipal();
        assertThat(principal.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void authenticateShouldRejectInvalidPassword() {
        User user = createUser("ACTIVE");
        passwordCredentialService.createCredential(user.getId(), "correct-password");

        PasswordAuthentication auth = PasswordAuthentication.ofCredentials(user.getEmail(), "wrong-password");
        assertThatThrownBy(() -> passwordAuthenticationProvider.authenticate(auth))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateShouldRejectNonexistentEmail() {
        PasswordAuthentication auth = PasswordAuthentication.ofCredentials("nobody@example.com", "any-password");
        assertThatThrownBy(() -> passwordAuthenticationProvider.authenticate(auth))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateShouldRejectUserWithoutPasswordCredential() {
        User user = createUser("ACTIVE");

        PasswordAuthentication auth = PasswordAuthentication.ofCredentials(user.getEmail(), "any-password");
        assertThatThrownBy(() -> passwordAuthenticationProvider.authenticate(auth))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateShouldCanonicalizeEmailToLowercase() {
        User user = createUser("ACTIVE");
        passwordCredentialService.createCredential(user.getId(), "my-password");

        String upperCaseEmail = user.getEmail().toUpperCase();
        PasswordAuthentication auth = PasswordAuthentication.ofCredentials(upperCaseEmail, "my-password");
        Authentication result = passwordAuthenticationProvider.authenticate(auth);

        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    void authenticateShouldRejectInactiveUser() {
        User user = createUser("SUSPENDED");
        passwordCredentialService.createCredential(user.getId(), "my-password");

        PasswordAuthentication auth = PasswordAuthentication.ofCredentials(user.getEmail(), "my-password");
        assertThatThrownBy(() -> passwordAuthenticationProvider.authenticate(auth))
            .isInstanceOf(BadCredentialsException.class);
    }

    private User createUser(String status) {
        return userService.createUser("auth-" + UUID.randomUUID() + "@example.com", status);
    }
}
