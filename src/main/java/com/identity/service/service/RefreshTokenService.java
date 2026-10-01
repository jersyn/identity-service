package com.identity.service.service;

import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.persistence.repository.RefreshTokenRepository;
import com.identity.service.persistence.repository.TokenFamilyRepository;
import com.identity.service.security.JwtAccessTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 SecureRandom not available", e);
        }
    }

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenFamilyRepository tokenFamilyRepository;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final JwtAccessTokenProperties jwtAccessTokenProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenFamilyRepository tokenFamilyRepository,
            AccessTokenService accessTokenService,
            RefreshTokenProperties refreshTokenProperties,
            JwtAccessTokenProperties jwtAccessTokenProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenFamilyRepository = tokenFamilyRepository;
        this.accessTokenService = accessTokenService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.jwtAccessTokenProperties = jwtAccessTokenProperties;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenExchangeResponse exchangeRefreshToken(String opaqueRefreshToken) {
        byte[] tokenBytes = HEX_FORMAT.parseHex(opaqueRefreshToken);
        byte[] tokenHash = sha256(tokenBytes);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidRefreshTokenException("invalid refresh token"));

        if (!"ACTIVE".equals(token.getStatus())) {
            revokeFamily(token.getTokenFamilyId());
            throw new InvalidRefreshTokenException("invalid refresh token");
        }

        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException("invalid refresh token");
        }

        TokenFamily family = tokenFamilyRepository.findById(token.getTokenFamilyId())
            .orElseThrow(() -> new InvalidRefreshTokenException("invalid refresh token"));

        if (!"ACTIVE".equals(family.getStatus())) {
            throw new InvalidRefreshTokenException("invalid refresh token");
        }

        int rows = refreshTokenRepository.markAsUsed(token.getId());
        if (rows == 0) {
            throw new InvalidRefreshTokenException("invalid refresh token");
        }

        OpaqueToken newOpaqueToken = generateOpaqueToken();

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setTokenFamilyId(family.getId());
        newRefreshToken.setStatus("ACTIVE");
        newRefreshToken.setExpiresAt(OffsetDateTime.now().plus(refreshTokenProperties.idleTimeout()));
        newRefreshToken.setTokenHash(newOpaqueToken.hash);
        refreshTokenRepository.create(newRefreshToken);

        String accessToken = accessTokenService.issue(family.getUserId().toString());
        long expiresIn = jwtAccessTokenProperties.lifetime().getSeconds();

        return new TokenExchangeResponse(accessToken, newOpaqueToken.value, expiresIn);
    }

    private void revokeFamily(UUID familyId) {
        tokenFamilyRepository.revoke(familyId);
        refreshTokenRepository.revokeActiveByFamilyId(familyId);
    }

    private OpaqueToken generateOpaqueToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String opaqueValue = HEX_FORMAT.formatHex(tokenBytes);
        byte[] hash = sha256(tokenBytes);
        return new OpaqueToken(opaqueValue, hash);
    }

    private byte[] sha256(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record OpaqueToken(String value, byte[] hash) {
    }
}
