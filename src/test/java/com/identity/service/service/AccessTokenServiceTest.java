package com.identity.service.service;

import com.identity.service.security.JwtAccessTokenProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenServiceTest {

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private AccessTokenService service;

    private static final String TEST_KID = "test-kid-1";
    private static final String TEST_ISSUER = "test-issuer";
    private static final String TEST_AUDIENCE = "test-audience";
    private static final Duration TEST_LIFETIME = Duration.ofMinutes(15);

    @BeforeEach
    void setUp() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            privateKey = (RSAPrivateKey) kp.getPrivate();
            publicKey = (RSAPublicKey) kp.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JwtAccessTokenProperties properties = new JwtAccessTokenProperties(
            TEST_ISSUER, TEST_AUDIENCE, TEST_LIFETIME);
        service = new AccessTokenService(privateKey, TEST_KID, properties);
    }

    @Test
    void shouldIssueNonEmptyJwt() {
        String jwt = service.issue("user-123");
        assertThat(jwt).isNotBlank();
        assertThat(jwt.split("\\.")).hasSize(3);
    }

    @Test
    void shouldContainCorrectSubject() {
        String jwt = service.issue("user-456");
        Claims claims = parseAndVerify(jwt);
        assertThat(claims.getSubject()).isEqualTo("user-456");
    }

    @Test
    void shouldContainCorrectIssuer() {
        String jwt = service.issue("user-123");
        Claims claims = parseAndVerify(jwt);
        assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
    }

    @Test
    void shouldContainCorrectAudience() {
        String jwt = service.issue("user-123");
        Claims claims = parseAndVerify(jwt);
        assertThat(claims.getAudience()).containsExactly(TEST_AUDIENCE);
    }

    @Test
    void shouldContainIssuedAt() {
        Instant before = Instant.now();
        String jwt = service.issue("user-123");
        Instant after = Instant.now();

        Claims claims = parseAndVerify(jwt);
        Instant iat = claims.getIssuedAt().toInstant();
        assertThat(iat).isAfter(before.minusSeconds(1));
        assertThat(iat).isBefore(after.plusSeconds(1));
    }

    @Test
    void shouldContainExpirationApproximately15MinutesAfterIssuedAt() {
        Instant before = Instant.now();
        String jwt = service.issue("user-123");
        Instant after = Instant.now();

        Claims claims = parseAndVerify(jwt);
        Instant iat = claims.getIssuedAt().toInstant();
        Instant exp = claims.getExpiration().toInstant();

        assertThat(exp).isEqualTo(iat.plus(TEST_LIFETIME));

        Instant expectedMinExp = before.plus(TEST_LIFETIME).minusSeconds(1);
        Instant expectedMaxExp = after.plus(TEST_LIFETIME).plusSeconds(1);
        assertThat(exp).isAfter(expectedMinExp);
        assertThat(exp).isBefore(expectedMaxExp);
    }

    @Test
    void shouldContainUniqueIdentifier() {
        String jwt1 = service.issue("user-123");
        String jwt2 = service.issue("user-123");

        Claims claims1 = parseAndVerify(jwt1);
        Claims claims2 = parseAndVerify(jwt2);

        assertThat(claims1.getId()).isNotBlank();
        assertThat(claims2.getId()).isNotBlank();
        assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
    }

    @Test
    void shouldHaveKidInHeader() {
        String jwt = service.issue("user-123");
        Jws<Claims> parsed = Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(jwt);

        assertThat(parsed.getHeader().getKeyId()).isEqualTo(TEST_KID);
    }

    @Test
    void shouldVerifySignatureWithPublicKey() {
        String jwt = service.issue("user-123");
        Claims claims = parseAndVerify(jwt);
        assertThat(claims).isNotNull();
    }

    private Claims parseAndVerify(String jwt) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(jwt)
            .getPayload();
    }
}
