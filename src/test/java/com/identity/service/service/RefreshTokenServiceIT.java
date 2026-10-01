package com.identity.service.service;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.domain.User;
import com.identity.service.persistence.repository.RefreshTokenRepository;
import com.identity.service.persistence.repository.TokenFamilyRepository;
import com.identity.service.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenFamilyRepository tokenFamilyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void exchangeShouldRotateRefreshTokenAndIssueAccessToken() {
        User user = createUser();
        TokenFamily family = createFamily(user.getId());
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);
        createActiveToken(family.getId(), tokenHash);

        TokenExchangeResponse response = refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.refreshToken()).isNotEqualTo(HEX.formatHex(tokenBytes));

        RefreshToken consumed = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(consumed.getStatus()).isEqualTo("USED");
        assertThat(consumed.getUsedAt()).isNotNull();
    }

    @Test
    void exchangeShouldRejectAlreadyConsumedToken() {
        User user = createUser();
        TokenFamily family = createFamily(user.getId());
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);
        RefreshToken token = createActiveToken(family.getId(), tokenHash);
        refreshTokenRepository.markAsUsed(token.getId());

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);

        TokenFamily revoked = tokenFamilyRepository.findById(family.getId()).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.getRevokedAt()).isNotNull();
    }

    @Test
    void replayShouldRevokeFamilyAndBlockSubsequentRefreshes() {
        User user = createUser();
        TokenFamily family = createFamily(user.getId());
        byte[] originalBytes = randomBytes();
        createActiveToken(family.getId(), sha256(originalBytes));

        TokenExchangeResponse rotation = refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(originalBytes));

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(originalBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);

        TokenFamily revoked = tokenFamilyRepository.findById(family.getId()).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.getRevokedAt()).isNotNull();

        var tokensAfterReuse = refreshTokenRepository.findAllByTokenFamilyId(family.getId());
        assertThat(tokensAfterReuse).isNotEmpty();
        assertThat(tokensAfterReuse).noneMatch(t -> "ACTIVE".equals(t.getStatus()));

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            rotation.refreshToken()))
            .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(refreshTokenRepository.findAllByTokenFamilyId(family.getId()))
            .hasSameSizeAs(tokensAfterReuse);
    }

    @Test
    void exchangeShouldRejectExpiredToken() {
        User user = createUser();
        TokenFamily family = createFamily(user.getId());
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setTokenFamilyId(family.getId());
        expiredToken.setStatus("ACTIVE");
        expiredToken.setExpiresAt(OffsetDateTime.now().minusHours(1));
        expiredToken.setTokenHash(tokenHash);
        refreshTokenRepository.create(expiredToken);

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(tokenFamilyRepository.findById(family.getId()).orElseThrow().getStatus())
            .isEqualTo("ACTIVE");
    }

    @Test
    void exchangeShouldRejectUnknownToken() {
        byte[] tokenBytes = randomBytes();

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private User createUser() {
        User user = new User();
        user.setEmail("refreshsvc-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        return userRepository.create(user);
    }

    private TokenFamily createFamily(UUID userId) {
        TokenFamily family = new TokenFamily();
        family.setUserId(userId);
        family.setStatus("ACTIVE");
        return tokenFamilyRepository.create(family);
    }

    private RefreshToken createActiveToken(UUID familyId, byte[] hash) {
        RefreshToken token = new RefreshToken();
        token.setTokenFamilyId(familyId);
        token.setStatus("ACTIVE");
        token.setExpiresAt(OffsetDateTime.now().plusDays(30));
        token.setTokenHash(hash);
        return refreshTokenRepository.create(token);
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
