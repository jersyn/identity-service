package com.identity.service.security;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
class JwksController {

    private final Map<String, Object> jwksBody;

    JwksController(JwkSet jwkSet) {
        this.jwksBody = buildResponseBody(jwkSet);
    }

    @GetMapping(value = "/.well-known/jwks.json",
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(jwksBody);
    }

    private static Map<String, Object> buildResponseBody(JwkSet jwkSet) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> keys = new ArrayList<>();
        for (Jwk<?> jwk : jwkSet.getKeys()) {
            keys.add(new LinkedHashMap<>(jwk));
        }
        body.put("keys", keys);
        return body;
    }
}
