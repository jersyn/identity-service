package com.identity.service.security;

import com.identity.service.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwksControllerIT extends AbstractIntegrationTest {

    private static final String JWKS_PATH = "/.well-known/jwks.json";

    @LocalServerPort
    private int port;

    @Autowired
    private java.security.interfaces.RSAPublicKey signingPublicKey;

    @Autowired
    private String signingKid;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void getJwksShouldReturn200() {
        ResponseEntity<Map<String, Object>> response = getJwksEntity();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void getJwksShouldNotRequireAuthentication() {
        RestClient unauthenticated = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
        ResponseEntity<Map<String, Object>> response = unauthenticated.get()
            .uri(JWKS_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void getJwksShouldReturnJson() {
        ResponseEntity<Map<String, Object>> response = getJwksEntity();
        assertThat(response.getHeaders().getContentType())
            .isNotNull()
            .satisfies(ct -> assertThat(ct.toString()).contains("application/json"));
    }

    @Test
    void responseShouldContainKeysArray() {
        Map<String, Object> body = getJwks();
        assertThat(body.get("keys")).isInstanceOf(List.class);
    }

    @Test
    void keysShouldContainExactlyOneEntry() {
        Map<String, Object> body = getJwks();
        List<?> keys = (List<?>) body.get("keys");
        assertThat(keys).hasSize(1);
    }

    @Test
    void keyShouldHaveRsaKeyType() {
        Map<String, Object> key = getFirstKey();
        assertThat(key.get("kty")).isEqualTo("RSA");
    }

    @Test
    void keyShouldMatchConfiguredKid() {
        Map<String, Object> key = getFirstKey();
        assertThat(key.get("kid")).isEqualTo(signingKid);
    }

    @Test
    void keyShouldHaveSigUse() {
        Map<String, Object> key = getFirstKey();
        assertThat(key.get("use")).isEqualTo("sig");
    }

    @Test
    void keyShouldHaveRs256Algorithm() {
        Map<String, Object> key = getFirstKey();
        assertThat(key.get("alg")).isEqualTo("RS256");
    }

    @Test
    void keyShouldHaveModulusAndExponent() {
        Map<String, Object> key = getFirstKey();
        assertThat((String) key.get("n")).isNotBlank();
        assertThat((String) key.get("e")).isNotBlank();
    }

    @Test
    void publishedModulusShouldMatchConfiguredKey() {
        Map<String, Object> key = getFirstKey();
        BigInteger publishedModulus = new BigInteger(1,
            Base64.getUrlDecoder().decode((String) key.get("n")));
        assertThat(publishedModulus).isEqualTo(signingPublicKey.getModulus());
    }

    @Test
    void publishedExponentShouldMatchConfiguredKey() {
        Map<String, Object> key = getFirstKey();
        BigInteger publishedExponent = new BigInteger(1,
            Base64.getUrlDecoder().decode((String) key.get("e")));
        assertThat(publishedExponent).isEqualTo(signingPublicKey.getPublicExponent());
    }

    @Test
    void responseShouldNotContainPrivateKeyParams() {
        Map<String, Object> key = getFirstKey();
        Set<String> allowedKeys = Set.of("kid", "use", "alg", "kty", "n", "e");
        assertThat(key.keySet()).isEqualTo(allowedKeys);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJwks() {
        return getJwksEntity().getBody();
    }

    private ResponseEntity<Map<String, Object>> getJwksEntity() {
        return restClient.get()
            .uri(JWKS_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFirstKey() {
        Map<String, Object> body = getJwks();
        List<Map<String, Object>> keys = (List<Map<String, Object>>) body.get("keys");
        return keys.get(0);
    }
}
