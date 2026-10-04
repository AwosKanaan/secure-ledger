package org.secureledger.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private BigDecimal amount;

    private String currency;

    private String description;

    private String counterpartyIban;

    private UUID idempotencyKey;

    private Instant createdAt;

    public Transaction(User user, UUID idempotencyKey, BigDecimal amount, String currency, String description,
                       String counterpartyIban, Instant createdAt) {
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.counterpartyIban = counterpartyIban;
        this.createdAt = createdAt;
    }
}
