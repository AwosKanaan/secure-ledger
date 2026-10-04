package org.secureledger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.secureledger.dao.TransactionDAO;
import org.secureledger.dao.UserDAO;
import org.secureledger.dto.CreateTransactionRequest;
import org.secureledger.exception.IdempotencyKeyReusedException;
import org.secureledger.model.Transaction;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID KEY = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Pageable PAGE = Pageable.ofSize(20);
    private static final CreateTransactionRequest REQUEST =
            new CreateTransactionRequest(new BigDecimal("99.00"), "EUR", "Invoice", "DE89370400440532013000");

    @Mock TransactionDAO repository;
    @Mock UserDAO users;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void record_newKey_savesTransaction() {
        when(repository.findByUserIdAndIdempotencyKey(USER, KEY)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.record(USER, KEY, REQUEST).amount()).isEqualByComparingTo("99.00");
    }

    @Test
    void record_sameKeySamePayload_returnsOriginalWithoutSaving() {
        when(repository.findByUserIdAndIdempotencyKey(USER, KEY)).thenReturn(Optional.of(stored("99.0000")));

        service.record(USER, KEY, REQUEST);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void record_sameKeyDifferentPayload_throwsIdempotencyKeyReused() {
        when(repository.findByUserIdAndIdempotencyKey(USER, KEY)).thenReturn(Optional.of(stored("999.0000")));

        assertThatThrownBy(() -> service.record(USER, KEY, REQUEST)).isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void history_noFilters_returnsPage() {
        when(repository.findUserTransactionHistory(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(stored("99.0000"))));

        assertThat(service.history(USER, null, null, PAGE).totalElements()).isEqualTo(1);
    }

    @Test
    void history_startAfterEnd_throwsBadRequest() {
        LocalDate march = LocalDate.of(2026, 3, 1);

        assertThatThrownBy(() -> service.history(USER, march, march.minusMonths(1), PAGE))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void history_unsupportedSortField_throwsBadRequest() {
        Pageable byEmail = PageRequest.of(0, 20, Sort.by("user.email"));

        assertThatThrownBy(() -> service.history(USER, null, null, byEmail))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static Transaction stored(String amount) {
        return new Transaction(null, KEY, new BigDecimal(amount), "EUR", "Invoice", "DE89370400440532013000", NOW);
    }
}
