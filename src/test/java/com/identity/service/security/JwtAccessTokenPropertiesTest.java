package com.identity.service.security;

import com.identity.service.service.AccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JwtSigningConfig.class, JwtAccessTokenConfig.class));

    @Test
    void shouldCreateBeanWithValidProperties() {
        java.security.KeyPair kp = generateKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + java.util.Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + java.util.Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid",
                "jwt.access-token.issuer=test-issuer",
                "jwt.access-token.audience=test-audience",
                "jwt.access-token.lifetime=PT15M"
            )
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(AccessTokenService.class);
            });
    }

    @Test
    void shouldFailWhenAudienceIsBlank() {
        assertThatThrownBy(() -> new JwtAccessTokenProperties(
            "test-issuer", "", Duration.ofMinutes(15)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jwt.access-token.audience must not be blank");
    }

    @Test
    void shouldFailWhenAudienceIsNull() {
        assertThatThrownBy(() -> new JwtAccessTokenProperties(
            "test-issuer", null, Duration.ofMinutes(15)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jwt.access-token.audience must not be blank");
    }

    @Test
    void shouldDefaultIssuerWhenBlank() {
        JwtAccessTokenProperties properties = new JwtAccessTokenProperties(
            "", "test-audience", Duration.ofMinutes(15));
        assertThat(properties.issuer()).isEqualTo("identity-service");
    }

    @Test
    void shouldDefaultIssuerWhenNull() {
        JwtAccessTokenProperties properties = new JwtAccessTokenProperties(
            null, "test-audience", Duration.ofMinutes(15));
        assertThat(properties.issuer()).isEqualTo("identity-service");
    }

    @Test
    void shouldFailWhenLifetimeIsNegative() {
        assertThatThrownBy(() -> new JwtAccessTokenProperties(
            "test-issuer", "test-audience", Duration.ofMinutes(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jwt.access-token.lifetime must be a positive duration");
    }

    @Test
    void shouldFailWhenLifetimeIsZero() {
        assertThatThrownBy(() -> new JwtAccessTokenProperties(
            "test-issuer", "test-audience", Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jwt.access-token.lifetime must be a positive duration");
    }

    @Test
    void shouldFailWhenLifetimeIsNull() {
        assertThatThrownBy(() -> new JwtAccessTokenProperties(
            "test-issuer", "test-audience", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jwt.access-token.lifetime must be a positive duration");
    }

    private static java.security.KeyPair generateKeyPair() {
        try {
            java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
