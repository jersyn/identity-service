package com.identity.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.RefreshToken;
import com.identity.service.domain.TokenFamily;
import com.identity.service.domain.User;
import com.identity.service.persistence.repository.RefreshTokenRepository;
import com.identity.service.persistence.repository.TokenFamilyRepository;
import com.identity.service.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TokenControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenFamilyRepository tokenFamilyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RestClient restClient;
    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void postTokenWithValidRefreshTokenShouldReturn200() {
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);
        setupToken(tokenHash);

        ResponseEntity<Map> response = exchangeToken(HEX.formatHex(tokenBytes));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("access_token")).isNotNull();
        assertThat(body.get("refresh_token")).isNotNull();
        assertThat(body.get("expires_in")).isNotNull();
    }

    @Test
    void postTokenWithInvalidRefreshTokenShouldReturn400() {
        byte[] tokenBytes = randomBytes();

        ResponseEntity<Map> response = exchangeToken(HEX.formatHex(tokenBytes));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("invalid_refresh_token");
    }

    @Test
    void postTokenWithMissingBodyShouldReturn400() {
        ResponseEntity<Map> response = exchangeTokenRaw(Map.of());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("invalid_refresh_token");
    }

    @Test
    void postTokenShouldNotExposeTokenExistence() {
        ResponseEntity<Map> unknownResponse = exchangeToken(HEX.formatHex(randomBytes()));
        assertThat(unknownResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(unknownResponse.getBody()).containsEntry("error", "invalid_refresh_token");

        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);
        RefreshToken token = setupToken(tokenHash);
        refreshTokenRepository.markAsUsed(token.getId());

        ResponseEntity<Map> usedResponse = exchangeToken(HEX.formatHex(tokenBytes));
        assertThat(usedResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(usedResponse.getBody()).containsEntry("error", "invalid_refresh_token");
    }

    @Test
    void concurrentExchangeShouldSucceedExactlyOnce() throws Exception {
        byte[] tokenBytes = randomBytes();
        byte[] tokenHash = sha256(tokenBytes);
        RefreshToken token = setupToken(tokenHash);
        String opaqueToken = HEX.formatHex(tokenBytes);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<ResponseEntity<Map>> future1 = CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            await(start);
            return exchangeToken(opaqueToken);
        }, executor);

        CompletableFuture<ResponseEntity<Map>> future2 = CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            await(start);
            return exchangeToken(opaqueToken);
        }, executor);

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        ResponseEntity<Map> response1 = future1.get(10, TimeUnit.SECONDS);
        ResponseEntity<Map> response2 = future2.get(10, TimeUnit.SECONDS);

        long successCount = java.util.List.of(response1, response2).stream()
            .filter(r -> r.getStatusCode().is2xxSuccessful())
            .count();
        long failureCount = java.util.List.of(response1, response2).stream()
            .filter(r -> r.getStatusCode().value() == 400)
            .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        assertThat(refreshTokenRepository.findByTokenHash(tokenHash)).isEmpty();

        assertThat(refreshTokenRepository.findById(token.getId())).isPresent();

        executor.shutdown();
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> exchangeToken(String opaqueToken) {
        return exchangeTokenRaw(Map.of("refresh_token", opaqueToken));
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> exchangeTokenRaw(Map<String, String> body) {
        try {
            return restClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
        } catch (HttpClientErrorException e) {
            try {
                Map errorBody = MAPPER.readValue(e.getResponseBodyAsString(), Map.class);
                return ResponseEntity.status(e.getStatusCode().value()).body(errorBody);
            } catch (Exception parseEx) {
                return ResponseEntity.status(e.getStatusCode().value()).build();
            }
        }
    }

    private RefreshToken setupToken(byte[] tokenHash) {
        User user = new User();
        user.setEmail("controller-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        User createdUser = userRepository.create(user);

        TokenFamily family = new TokenFamily();
        family.setUserId(createdUser.getId());
        family.setStatus("ACTIVE");
        TokenFamily createdFamily = tokenFamilyRepository.create(family);

        RefreshToken token = new RefreshToken();
        token.setTokenFamilyId(createdFamily.getId());
        token.setStatus("ACTIVE");
        token.setExpiresAt(OffsetDateTime.now().plusDays(30));
        token.setTokenHash(tokenHash);
        return refreshTokenRepository.create(token);
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
