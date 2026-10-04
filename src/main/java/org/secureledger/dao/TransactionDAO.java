package org.secureledger.dao;

import org.secureledger.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransactionDAO extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    @Query("""
            select t from Transaction t
            where t.user.id = :userId and t.createdAt >= :from and t.createdAt < :to
            """)
    Page<Transaction> findUserTransactionHistory(UUID userId, Instant from, Instant to, Pageable pageable);
}
