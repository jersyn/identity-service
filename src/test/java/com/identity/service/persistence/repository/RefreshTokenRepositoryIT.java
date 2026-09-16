package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenFamilyRepository tokenFamilyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void createShouldPersistRefreshToken() {
        TokenFamily tokenFamily = createTokenFamily();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamily.getId());
        refreshToken.setStatus("ACTIVE");
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        refreshToken.setTokenHash(randomHash());

        RefreshToken created = refreshTokenRepository.create(refreshToken);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTokenFamilyId()).isEqualTo(tokenFamily.getId());
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getExpiresAt()).isNotNull();
        assertThat(created.getUsedAt()).isNull();
        assertThat(created.getTokenHash()).isNotNull();
    }

    @Test
    void findByIdShouldReturnExistingRefreshToken() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken refreshToken = createRefreshToken(tokenFamily.getId(), "ACTIVE");

        Optional<RefreshToken> found = refreshTokenRepository.findById(refreshToken.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(refreshToken.getId());
        assertThat(found.get().getTokenFamilyId()).isEqualTo(tokenFamily.getId());
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findByIdShouldReturnEmptyForNonexistentId() {
        Optional<RefreshToken> found = refreshTokenRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void createShouldPersistUsedStatus() {
        TokenFamily tokenFamily = createTokenFamily();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamily.getId());
        refreshToken.setStatus("USED");
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        refreshToken.setUsedAt(OffsetDateTime.now());
        refreshToken.setTokenHash(randomHash());

        RefreshToken created = refreshTokenRepository.create(refreshToken);

        assertThat(created.getStatus()).isEqualTo("USED");
        assertThat(created.getUsedAt()).isNotNull();
    }

    @Test
    void createShouldPersistRevokedStatus() {
        TokenFamily tokenFamily = createTokenFamily();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamily.getId());
        refreshToken.setStatus("REVOKED");
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        refreshToken.setTokenHash(randomHash());

        RefreshToken created = refreshTokenRepository.create(refreshToken);

        assertThat(created.getStatus()).isEqualTo("REVOKED");
    }

    @Test
    void createShouldPersistExpiresAt() {
        TokenFamily tokenFamily = createTokenFamily();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamily.getId());
        refreshToken.setStatus("ACTIVE");
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setTokenHash(randomHash());

        RefreshToken created = refreshTokenRepository.create(refreshToken);

        assertThat(created.getExpiresAt()).isEqualToIgnoringNanos(expiresAt);
    }

    @Test
    void createShouldGenerateUniqueIds() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken token1 = createRefreshToken(tokenFamily.getId(), "ACTIVE");
        RefreshToken token2 = createRefreshToken(tokenFamily.getId(), "ACTIVE");

        assertThat(token1.getId()).isNotEqualTo(token2.getId());
    }

    @Test
    void createShouldAllowMultipleTokensPerFamily() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken token1 = createRefreshToken(tokenFamily.getId(), "ACTIVE");
        RefreshToken token2 = createRefreshToken(tokenFamily.getId(), "USED");

        assertThat(token1.getId()).isNotEqualTo(token2.getId());
        assertThat(token1.getTokenFamilyId()).isEqualTo(tokenFamily.getId());
        assertThat(token2.getTokenFamilyId()).isEqualTo(tokenFamily.getId());
    }

    @Test
    void findByTokenHashShouldReturnExistingToken() {
        TokenFamily tokenFamily = createTokenFamily();
        byte[] hash = randomHash();
        RefreshToken token = createRefreshTokenWithHash(tokenFamily.getId(), "ACTIVE", hash);

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(token.getId());
    }

    @Test
    void findByTokenHashShouldReturnEmptyForNonexistentHash() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(randomHash());

        assertThat(found).isEmpty();
    }

    @Test
    void markAsUsedShouldTransitionActiveTokenToUsed() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken token = createRefreshToken(tokenFamily.getId(), "ACTIVE");

        int rows = refreshTokenRepository.markAsUsed(token.getId());

        assertThat(rows).isEqualTo(1);
        RefreshToken updated = refreshTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("USED");
        assertThat(updated.getUsedAt()).isNotNull();
    }

    @Test
    void markAsUsedShouldReturnZeroForAlreadyUsedToken() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken token = createRefreshToken(tokenFamily.getId(), "ACTIVE");
        refreshTokenRepository.markAsUsed(token.getId());

        int rows = refreshTokenRepository.markAsUsed(token.getId());

        assertThat(rows).isEqualTo(0);
    }

    @Test
    void markAsUsedShouldReturnZeroForExpiredToken() {
        TokenFamily tokenFamily = createTokenFamily();
        RefreshToken token = new RefreshToken();
        token.setTokenFamilyId(tokenFamily.getId());
        token.setStatus("ACTIVE");
        token.setExpiresAt(OffsetDateTime.now().minusHours(1));
        token.setTokenHash(randomHash());
        RefreshToken created = refreshTokenRepository.create(token);

        int rows = refreshTokenRepository.markAsUsed(created.getId());

        assertThat(rows).isEqualTo(0);
    }

    private TokenFamily createTokenFamily() {
        User user = new User();
        user.setEmail("refreshtoken-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        User createdUser = userRepository.create(user);

        TokenFamily tokenFamily = new TokenFamily();
        tokenFamily.setUserId(createdUser.getId());
        tokenFamily.setStatus("ACTIVE");
        return tokenFamilyRepository.create(tokenFamily);
    }

    private RefreshToken createRefreshToken(UUID tokenFamilyId, String status) {
        return createRefreshTokenWithHash(tokenFamilyId, status, randomHash());
    }

    private RefreshToken createRefreshTokenWithHash(UUID tokenFamilyId, String status, byte[] hash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamilyId);
        refreshToken.setStatus(status);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        refreshToken.setTokenHash(hash);
        return refreshTokenRepository.create(refreshToken);
    }

    private byte[] randomHash() {
        try {
            byte[] bytes = new byte[32];
            new java.security.SecureRandom().nextBytes(bytes);
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
