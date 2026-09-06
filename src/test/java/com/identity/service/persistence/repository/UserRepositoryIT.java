package com.identity.service.persistence.repository;

import com.identity.service.AbstractIntegrationTest;
import com.identity.service.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void createShouldPersistUser() {
        User user = new User();
        user.setStatus("ACTIVE");

        User created = userRepository.create(user);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByIdShouldReturnExistingUser() {
        User user = new User();
        user.setStatus("ACTIVE");
        User created = userRepository.create(user);

        Optional<User> found = userRepository.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findByIdShouldReturnEmptyForNonexistentId() {
        Optional<User> found = userRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void updateShouldModifyUserStatus() {
        User user = new User();
        user.setStatus("ACTIVE");
        User created = userRepository.create(user);

        created.setStatus("SUSPENDED");
        User updated = userRepository.update(created);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void createShouldGenerateUniqueIds() {
        User user1 = new User();
        user1.setStatus("ACTIVE");
        User created1 = userRepository.create(user1);

        User user2 = new User();
        user2.setStatus("ACTIVE");
        User created2 = userRepository.create(user2);

        assertThat(created1.getId()).isNotEqualTo(created2.getId());
    }
}
