package com.identity.service.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class TokenFamily {

    private UUID id;
    private UUID userId;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime revokedAt;

    public TokenFamily() {
    }

    public TokenFamily(UUID id, UUID userId, String status, OffsetDateTime createdAt, OffsetDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenFamily that = (TokenFamily) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TokenFamily{" +
            "id=" + id +
            ", userId=" + userId +
            ", status='" + status + '\'' +
            ", createdAt=" + createdAt +
            ", revokedAt=" + revokedAt +
            '}';
    }
}
