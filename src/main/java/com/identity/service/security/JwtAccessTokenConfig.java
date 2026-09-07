package com.identity.service.security;

import com.identity.service.service.AccessTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;

@Configuration
@EnableConfigurationProperties(JwtAccessTokenProperties.class)
class JwtAccessTokenConfig {

    @Bean
    AccessTokenService accessTokenService(
            RSAPrivateKey signingPrivateKey,
            String signingKid,
            JwtAccessTokenProperties properties) {
        return new AccessTokenService(signingPrivateKey, signingKid, properties);
    }
}
