package com.identity.service.service;

import com.identity.service.domain.User;
import com.identity.service.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(String status) {
        User user = new User();
        user.setStatus(status);
        return userRepository.create(user);
    }

    @Transactional
    public User updateStatus(UUID userId, String newStatus) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setStatus(newStatus);
        user.setUpdatedAt(OffsetDateTime.now());

        return userRepository.update(user);
    }
}
