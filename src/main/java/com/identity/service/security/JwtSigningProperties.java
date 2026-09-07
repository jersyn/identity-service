package com.identity.service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.signing")
public record JwtSigningProperties(
    String privateKey,
    String publicKey,
    String kid
) {
    public JwtSigningProperties {
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalArgumentException("jwt.signing.private-key must not be blank");
        }
        if (privateKey.startsWith("${")) {
            throw new IllegalArgumentException(
                "jwt.signing.private-key has an unresolved placeholder: " + privateKey);
        }
        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalArgumentException("jwt.signing.public-key must not be blank");
        }
        if (publicKey.startsWith("${")) {
            throw new IllegalArgumentException(
                "jwt.signing.public-key has an unresolved placeholder: " + publicKey);
        }
        if (kid == null || kid.isBlank()) {
            kid = "identity-service-key-1";
        }
    }
}
