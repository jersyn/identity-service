package com.identity.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSigningConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JwtSigningConfig.class));

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static boolean hasCauseInChain(Throwable throwable, Class<?> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    @Test
    void shouldCreateBeansWithValidKeyPair() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(RSAPrivateKey.class);
                assertThat(ctx).hasSingleBean(RSAPublicKey.class);
                assertThat(ctx).hasBean("signingKid");
                assertThat(ctx.getBean("signingKid", String.class)).isEqualTo("test-kid");
            });
    }

    @Test
    void shouldFailWithMismatchedKeyPair() {
        KeyPair kp1 = generateRsaKeyPair();
        KeyPair kp2 = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp1.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp2.getPublic().getEncoded()),
                "jwt.signing.kid=mismatch-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).satisfies(failure -> {
                    assertThat(hasCauseInChain(failure, IllegalStateException.class)).isTrue();
                    assertThat(failure).hasStackTraceContaining("RSA key pair mismatch");
                });
            });
    }

    @Test
    void shouldFailWithInvalidPrivateKey() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64("not-a-real-key".getBytes()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=bad-priv-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).satisfies(failure -> {
                    assertThat(hasCauseInChain(failure, IllegalStateException.class)).isTrue();
                    assertThat(failure).hasStackTraceContaining("Invalid RSA private key");
                });
            });
    }

    @Test
    void shouldFailWithInvalidPublicKey() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64("not-a-real-key".getBytes()),
                "jwt.signing.kid=bad-pub-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).satisfies(failure -> {
                    assertThat(hasCauseInChain(failure, IllegalStateException.class)).isTrue();
                    assertThat(failure).hasStackTraceContaining("Invalid RSA public key");
                });
            });
    }

    @Test
    void shouldDefaultKidWhenNotProvided() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded())
            )
            .run(ctx -> {
                assertThat(ctx).hasBean("signingKid");
                assertThat(ctx.getBean("signingKid", String.class)).isEqualTo("identity-service-key-1");
            });
    }

    @Test
    void shouldFailWithUnresolvedPrivateKeyPlaceholder() {
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=${JWT_SIGNING_PRIVATE_KEY}",
                "jwt.signing.public-key=" + encodeBase64(generateRsaKeyPair().getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).satisfies(failure -> {
                    assertThat(hasCauseInChain(failure, IllegalArgumentException.class)).isTrue();
                    assertThat(failure).hasStackTraceContaining("unresolved placeholder");
                });
            });
    }

    @Test
    void shouldFailWithUnresolvedPublicKeyPlaceholder() {
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(generateRsaKeyPair().getPrivate().getEncoded()),
                "jwt.signing.public-key=${JWT_SIGNING_PUBLIC_KEY}",
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).satisfies(failure -> {
                    assertThat(hasCauseInChain(failure, IllegalArgumentException.class)).isTrue();
                    assertThat(failure).hasStackTraceContaining("unresolved placeholder");
                });
            });
    }
}
