package com.identity.service.security;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwksConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JwtSigningConfig.class, JwksConfig.class));

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

    private static byte[] base64UrlDecode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    private static BigInteger modulusFromJwk(Jwk<?> jwk) {
        return new BigInteger(1, base64UrlDecode((String) jwk.get("n")));
    }

    private static BigInteger exponentFromJwk(Jwk<?> jwk) {
        return new BigInteger(1, base64UrlDecode((String) jwk.get("e")));
    }

    @Test
    void shouldCreateJwkSetBean() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(JwkSet.class);
            });
    }

    @Test
    void shouldContainExactlyOneKey() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                JwkSet jwkSet = ctx.getBean(JwkSet.class);
                assertThat(jwkSet.getKeys()).hasSize(1);
            });
    }

    @Test
    void shouldHaveRsaKeyType() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat(jwk.getType()).isEqualTo("RSA");
                assertThat((String) jwk.get("kty")).isEqualTo("RSA");
            });
    }

    @Test
    void shouldMatchConfiguredKid() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat(jwk.getId()).isEqualTo("test-kid");
            });
    }

    @Test
    void shouldHaveSigUse() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat((String) jwk.get("use")).isEqualTo("sig");
            });
    }

    @Test
    void shouldHaveRs256Algorithm() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat(jwk.getAlgorithm()).isEqualTo("RS256");
            });
    }

    @Test
    void shouldHaveModulus() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat((String) jwk.get("n")).isNotBlank();
            });
    }

    @Test
    void shouldHaveExponent() {
        KeyPair kp = generateRsaKeyPair();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(kp.getPublic().getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat((String) jwk.get("e")).isNotBlank();
            });
    }

    @Test
    void shouldMatchPublicKeyModulus() {
        KeyPair kp = generateRsaKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(pub.getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat(modulusFromJwk(jwk)).isEqualTo(pub.getModulus());
            });
    }

    @Test
    void shouldMatchPublicKeyExponent() {
        KeyPair kp = generateRsaKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        contextRunner
            .withPropertyValues(
                "jwt.signing.private-key=" + encodeBase64(kp.getPrivate().getEncoded()),
                "jwt.signing.public-key=" + encodeBase64(pub.getEncoded()),
                "jwt.signing.kid=test-kid"
            )
            .run(ctx -> {
                Jwk<?> jwk = ctx.getBean(JwkSet.class).getKeys().iterator().next();
                assertThat(exponentFromJwk(jwk)).isEqualTo(pub.getPublicExponent());
            });
    }
}
