package com.identity.service.health;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class DatabaseHealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public DatabaseHealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health/db")
    public ResponseEntity<Map<String, Object>> checkDbHealth() {
        Map<String, Object> status = new LinkedHashMap<>();

        // PostgreSQL check
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            status.put("postgresql", Map.of(
                "status", "UP",
                "database", meta.getDatabaseProductName(),
                "version", meta.getDatabaseProductVersion()
            ));
        } catch (Exception e) {
            status.put("postgresql", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }

        // Redis check
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            status.put("redis", Map.of(
                "status", "UP"
            ));
        } catch (Exception e) {
            status.put("redis", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }

        boolean allUp = status.values().stream()
            .allMatch(s -> s instanceof Map && "UP".equals(((Map<?, ?>) s).get("status")));

        return allUp
            ? ResponseEntity.ok(status)
            : ResponseEntity.status(503).body(status);
    }
}
