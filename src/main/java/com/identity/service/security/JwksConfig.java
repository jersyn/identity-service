package com.identity.service.security;

import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.RsaPublicJwk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPublicKey;

@Configuration
class JwksConfig {

    @Bean
    JwkSet jwkSet(RSAPublicKey signingPublicKey, String signingKid) {
        RsaPublicJwk jwk = (RsaPublicJwk) Jwks.builder()
            .key(signingPublicKey)
            .id(signingKid)
            .publicKeyUse("sig")
            .algorithm("RS256")
            .build();
        return Jwks.set().add(jwk).build();
    }
}
