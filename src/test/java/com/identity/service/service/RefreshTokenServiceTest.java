package com.identity.service.service;

import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.persistence.repository.RefreshTokenRepository;
import com.identity.service.persistence.repository.TokenFamilyRepository;
import com.identity.service.security.JwtAccessTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenFamilyRepository tokenFamilyRepository;

    private AccessTokenService accessTokenService;
    private RefreshTokenService refreshTokenService;
    private RSAPublicKey publicKey;

    private static final Duration IDLE_TIMEOUT = Duration.ofDays(30);
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom RANDOM = new SecureRandom();

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        JwtAccessTokenProperties jwtProps = new JwtAccessTokenProperties(
            "test-issuer", "test-audience", ACCESS_TOKEN_LIFETIME);
        accessTokenService = new AccessTokenService(privateKey, "test-kid", jwtProps);

        RefreshTokenProperties refreshProps = new RefreshTokenProperties(IDLE_TIMEOUT);
        JwtAccessTokenProperties jwtAccessTokenProperties = new JwtAccessTokenProperties(
            "test-issuer", "test-audience", ACCESS_TOKEN_LIFETIME);

        refreshTokenService = new RefreshTokenService(
            refreshTokenRepository, tokenFamilyRepository,
            accessTokenService, refreshProps, jwtAccessTokenProperties);
    }

    @Test
    void exchangeShouldReturnNewTokensOnValidRefreshToken() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken existingToken = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().plusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.markAsUsed(existingToken.getId())).thenReturn(1);

        TokenFamily family = new TokenFamily(
            familyId, userId, "ACTIVE", OffsetDateTime.now(), null);
        when(tokenFamilyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(refreshTokenRepository.create(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId(UUID.randomUUID());
            return rt;
        });

        TokenExchangeResponse response = refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(900);

        verify(refreshTokenRepository).markAsUsed(existingToken.getId());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).create(captor.capture());
        RefreshToken newToken = captor.getValue();
        assertThat(newToken.getStatus()).isEqualTo("ACTIVE");
        assertThat(newToken.getTokenFamilyId()).isEqualTo(familyId);
        assertThat(newToken.getTokenHash()).isNotNull();
        assertThat(newToken.getExpiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
    }

    @Test
    void exchangeShouldThrowOnUnknownTokenHash() {
        when(refreshTokenRepository.findByTokenHash(any(byte[].class)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(randomBytes())))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).markAsUsed(any());
    }

    @Test
    void exchangeShouldThrowOnAlreadyUsedToken() {
        UUID familyId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken usedToken = new RefreshToken(
            UUID.randomUUID(), familyId, "USED",
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now(), tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(usedToken));
        when(refreshTokenRepository.markAsUsed(usedToken.getId())).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenFamilyRepository, never()).findById(any());
    }

    @Test
    void exchangeShouldThrowOnExpiredToken() {
        UUID familyId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken expiredToken = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().minusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.markAsUsed(expiredToken.getId())).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void exchangeShouldThrowOnRevokedFamily() {
        UUID familyId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken token = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().plusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(token));
        when(refreshTokenRepository.markAsUsed(token.getId())).thenReturn(1);

        TokenFamily revokedFamily = new TokenFamily(
            familyId, UUID.randomUUID(), "REVOKED",
            OffsetDateTime.now(), OffsetDateTime.now());
        when(tokenFamilyRepository.findById(familyId))
            .thenReturn(Optional.of(revokedFamily));

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).create(any());
    }

    @Test
    void exchangeShouldThrowOnNonexistentFamily() {
        UUID familyId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken token = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().plusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(token));
        when(refreshTokenRepository.markAsUsed(token.getId())).thenReturn(1);
        when(tokenFamilyRepository.findById(familyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes)))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void newRefreshTokenShouldHaveCorrectExpiry() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken existingToken = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().plusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.markAsUsed(existingToken.getId())).thenReturn(1);

        TokenFamily family = new TokenFamily(
            familyId, userId, "ACTIVE", OffsetDateTime.now(), null);
        when(tokenFamilyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(refreshTokenRepository.create(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId(UUID.randomUUID());
            return rt;
        });

        OffsetDateTime before = OffsetDateTime.now();
        refreshTokenService.exchangeRefreshToken(HEX.formatHex(tokenBytes));
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).create(captor.capture());
        RefreshToken newToken = captor.getValue();

        OffsetDateTime expectedMin = before.plus(IDLE_TIMEOUT).minusSeconds(1);
        OffsetDateTime expectedMax = after.plus(IDLE_TIMEOUT).plusSeconds(1);
        assertThat(newToken.getExpiresAt()).isAfter(expectedMin);
        assertThat(newToken.getExpiresAt()).isBefore(expectedMax);
    }

    @Test
    void accessTokenShouldBeIssuedToFamilyOwner() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken existingToken = new RefreshToken(
            UUID.randomUUID(), familyId, "ACTIVE",
            OffsetDateTime.now().plusHours(1), null, tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.markAsUsed(existingToken.getId())).thenReturn(1);

        TokenFamily family = new TokenFamily(
            familyId, userId, "ACTIVE", OffsetDateTime.now(), null);
        when(tokenFamilyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(refreshTokenRepository.create(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId(UUID.randomUUID());
            return rt;
        });

        TokenExchangeResponse response = refreshTokenService.exchangeRefreshToken(
            HEX.formatHex(tokenBytes));

        assertThat(response.accessToken()).isNotBlank();
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(response.accessToken())
            .getPayload();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
    }

    private byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
