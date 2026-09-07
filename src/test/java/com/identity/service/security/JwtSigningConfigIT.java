package com.identity.service.security;

import com.identity.service.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSigningConfigIT extends AbstractIntegrationTest {

    @Autowired
    private RSAPrivateKey signingPrivateKey;

    @Autowired
    private RSAPublicKey signingPublicKey;

    @Autowired
    private String signingKid;

    @Test
    void signingPrivateKeyShouldBeCreated() {
        assertThat(signingPrivateKey).isNotNull();
        assertThat(signingPrivateKey).isInstanceOf(RSAPrivateCrtKey.class);
    }

    @Test
    void signingPublicKeyShouldBeCreated() {
        assertThat(signingPublicKey).isNotNull();
    }

    @Test
    void signingKidShouldMatchConfiguredValue() {
        assertThat(signingKid).isEqualTo("test-key-id");
    }

    @Test
    void publicKeyShouldMatchPrivateKey() {
        RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) signingPrivateKey;
        assertThat(signingPublicKey.getModulus()).isEqualTo(crtKey.getModulus());
        assertThat(signingPublicKey.getPublicExponent()).isEqualTo(crtKey.getPublicExponent());
    }
}
