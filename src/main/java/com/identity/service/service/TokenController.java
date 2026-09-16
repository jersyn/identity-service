package com.identity.service.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class TokenController {

    private final RefreshTokenService refreshTokenService;

    TokenController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> exchangeToken(@RequestBody TokenExchangeRequest request) {
        try {
            if (request.refreshToken() == null || request.refreshToken().isBlank()) {
                throw new InvalidRefreshTokenException("invalid refresh token");
            }
            TokenExchangeResponse response = refreshTokenService.exchangeRefreshToken(
                request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "invalid_refresh_token"));
        }
    }
}
