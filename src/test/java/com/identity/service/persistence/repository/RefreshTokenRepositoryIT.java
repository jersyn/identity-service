package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

        RefreshToken created = refreshTokenRepository.create(refreshToken);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTokenFamilyId()).isEqualTo(tokenFamily.getId());
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getExpiresAt()).isNotNull();
        assertThat(created.getUsedAt()).isNull();
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
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenFamilyId(tokenFamilyId);
        refreshToken.setStatus(status);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
        return refreshTokenRepository.create(refreshToken);
    }
}
