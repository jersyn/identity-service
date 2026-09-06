package com.identity.service.persistence.repository;

import com.identity.service.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    User create(User user);

    User update(User user);
}
