package com.identity.service.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt.refresh-token")
public record RefreshTokenProperties(Duration idleTimeout) {
    public RefreshTokenProperties {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException(
                "jwt.refresh-token.idle-timeout must be a positive duration");
        }
    }
}
