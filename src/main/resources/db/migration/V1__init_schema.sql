CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email           UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE transactions (
    id                UUID           PRIMARY KEY,
    user_id           UUID           NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idempotency_key   UUID           NOT NULL,
    amount            NUMERIC(19, 4) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    description       VARCHAR(140),
    counterparty_iban VARCHAR(34)    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_transactions_user_idempotency_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_transactions_amount_positive      CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency_format      CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_transactions_iban_format          CHECK (counterparty_iban ~ '^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$')
);

-- history by date, newest first
CREATE INDEX idx_transactions_user_created_at
    ON transactions (user_id, created_at DESC);

-- history sorted by amount
CREATE INDEX idx_transactions_user_amount
    ON transactions (user_id, amount);
