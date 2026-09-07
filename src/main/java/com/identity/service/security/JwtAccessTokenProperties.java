package com.identity.service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt.access-token")
public record JwtAccessTokenProperties(
    String issuer,
    String audience,
    Duration lifetime
) {
    public JwtAccessTokenProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "identity-service";
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("jwt.access-token.audience must not be blank");
        }
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("jwt.access-token.lifetime must be a positive duration");
        }
    }
}
