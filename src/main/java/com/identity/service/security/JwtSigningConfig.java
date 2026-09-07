package com.identity.service.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtSigningProperties.class)
class JwtSigningConfig {

    private static final String RSA = "RSA";

    @Bean
    RSAPrivateKey signingPrivateKey(JwtSigningProperties properties) {
        byte[] decoded = Base64.getDecoder().decode(stripPemHeaders(properties.privateKey()));
        RSAPrivateKey key = parsePrivateKey(decoded);
        validateKeyPair(key, parsePublicKey(Base64.getDecoder().decode(stripPemHeaders(properties.publicKey()))));
        return key;
    }

    @Bean
    RSAPublicKey signingPublicKey(JwtSigningProperties properties) {
        byte[] decoded = Base64.getDecoder().decode(stripPemHeaders(properties.publicKey()));
        return parsePublicKey(decoded);
    }

    @Bean
    String signingKid(JwtSigningProperties properties) {
        return properties.kid();
    }

    private RSAPrivateKey parsePrivateKey(byte[] keyBytes) {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance(RSA).generatePrivate(spec);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA private key", e);
        }
    }

    private RSAPublicKey parsePublicKey(byte[] keyBytes) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance(RSA).generatePublic(spec);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA public key", e);
        }
    }

    private void validateKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException("Private key must be an RSA CRT key");
        }
        if (!crtKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException("RSA key pair mismatch: moduli do not match");
        }
        if (!crtKey.getPublicExponent().equals(publicKey.getPublicExponent())) {
            throw new IllegalStateException("RSA key pair mismatch: public exponents do not match");
        }
    }

    private String stripPemHeaders(String key) {
        return key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    }
}
