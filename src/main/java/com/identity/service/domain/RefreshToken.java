package com.identity.service.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class RefreshToken {

    private UUID id;
    private UUID tokenFamilyId;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;

    public RefreshToken() {
    }

    public RefreshToken(UUID id, UUID tokenFamilyId, String status, OffsetDateTime expiresAt, OffsetDateTime usedAt) {
        this.id = id;
        this.tokenFamilyId = tokenFamilyId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public void setTokenFamilyId(UUID tokenFamilyId) {
        this.tokenFamilyId = tokenFamilyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
            "id=" + id +
            ", tokenFamilyId=" + tokenFamilyId +
            ", status='" + status + '\'' +
            ", expiresAt=" + expiresAt +
            ", usedAt=" + usedAt +
            '}';
    }
}
