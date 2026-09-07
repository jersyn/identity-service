package com.identity.service.service;

import com.identity.service.security.JwtAccessTokenProperties;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class AccessTokenService {

    private final RSAPrivateKey privateKey;
    private final String kid;
    private final String issuer;
    private final String audience;
    private final Duration lifetime;

    public AccessTokenService(
            RSAPrivateKey privateKey,
            String kid,
            JwtAccessTokenProperties properties) {
        this.privateKey = privateKey;
        this.kid = kid;
        this.issuer = properties.issuer();
        this.audience = properties.audience();
        this.lifetime = properties.lifetime();
    }

    public String issue(String subject) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plus(lifetime));

        return Jwts.builder()
            .header().keyId(kid).and()
            .issuer(issuer)
            .subject(subject)
            .audience().add(audience).and()
            .issuedAt(issuedAt)
            .expiration(expiration)
            .id(UUID.randomUUID().toString())
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }
}
