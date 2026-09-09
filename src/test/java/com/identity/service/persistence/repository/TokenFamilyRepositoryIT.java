package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.TokenFamily;
import com.identity.service.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenFamilyRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenFamilyRepository tokenFamilyRepository;

    @Test
    void createShouldPersistTokenFamily() {
        User user = createUser();

        TokenFamily tokenFamily = new TokenFamily();
        tokenFamily.setUserId(user.getId());
        tokenFamily.setStatus("ACTIVE");

        TokenFamily created = tokenFamilyRepository.create(tokenFamily);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(user.getId());
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getRevokedAt()).isNull();
    }

    @Test
    void findByIdShouldReturnExistingTokenFamily() {
        User user = createUser();
        TokenFamily tokenFamily = createTokenFamily(user.getId(), "ACTIVE");

        Optional<TokenFamily> found = tokenFamilyRepository.findById(tokenFamily.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(tokenFamily.getId());
        assertThat(found.get().getUserId()).isEqualTo(user.getId());
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findByIdShouldReturnEmptyForNonexistentId() {
        Optional<TokenFamily> found = tokenFamilyRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void createShouldPersistRevokedStatus() {
        User user = createUser();

        TokenFamily tokenFamily = new TokenFamily();
        tokenFamily.setUserId(user.getId());
        tokenFamily.setStatus("REVOKED");
        tokenFamily.setRevokedAt(OffsetDateTime.now());

        TokenFamily created = tokenFamilyRepository.create(tokenFamily);

        assertThat(created.getStatus()).isEqualTo("REVOKED");
        assertThat(created.getRevokedAt()).isNotNull();
    }

    @Test
    void createShouldGenerateUniqueIds() {
        User user = createUser();
        TokenFamily tokenFamily1 = createTokenFamily(user.getId(), "ACTIVE");
        TokenFamily tokenFamily2 = createTokenFamily(user.getId(), "ACTIVE");

        assertThat(tokenFamily1.getId()).isNotEqualTo(tokenFamily2.getId());
    }

    @Test
    void createShouldAllowMultipleFamiliesPerUser() {
        User user = createUser();
        TokenFamily family1 = createTokenFamily(user.getId(), "ACTIVE");
        TokenFamily family2 = createTokenFamily(user.getId(), "REVOKED");

        assertThat(family1.getId()).isNotEqualTo(family2.getId());
        assertThat(family1.getUserId()).isEqualTo(user.getId());
        assertThat(family2.getUserId()).isEqualTo(user.getId());
    }

    private User createUser() {
        User user = new User();
        user.setEmail("tokenfamily-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        return userRepository.create(user);
    }

    private TokenFamily createTokenFamily(UUID userId, String status) {
        TokenFamily tokenFamily = new TokenFamily();
        tokenFamily.setUserId(userId);
        tokenFamily.setStatus(status);
        return tokenFamilyRepository.create(tokenFamily);
    }
}
