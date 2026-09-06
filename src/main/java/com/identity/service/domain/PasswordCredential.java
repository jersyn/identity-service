package com.identity.service.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class PasswordCredential {

    public static final String ALGORITHM_ARGON2ID = "ARGON2ID";

    private UUID userId;
    private String passwordHash;
    private String algorithm;
    private OffsetDateTime updatedAt;

    public PasswordCredential() {
    }

    public PasswordCredential(UUID userId, String passwordHash, String algorithm, OffsetDateTime updatedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.algorithm = algorithm;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordCredential that = (PasswordCredential) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "PasswordCredential{" +
            "userId=" + userId +
            ", algorithm='" + algorithm + '\'' +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
