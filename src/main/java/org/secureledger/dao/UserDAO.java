package org.secureledger.dao;

import org.secureledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDAO extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
