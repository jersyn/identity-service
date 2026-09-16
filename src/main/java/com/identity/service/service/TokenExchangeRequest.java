package com.identity.service.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenExchangeRequest(
    @JsonProperty("refresh_token") String refreshToken
) {
}
