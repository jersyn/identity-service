package com.identity.service.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenExchangeResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") long expiresIn
) {
}
