package org.secureledger.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.secureledger.dto.CreateTransactionRequest;
import org.secureledger.security.AuthenticatedUser;
import org.secureledger.service.TransactionService;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock TransactionService service;
    @Mock AuthenticatedUser principal;
    @InjectMocks TransactionController controller;

    @BeforeEach
    void setUp() {
        when(principal.id()).thenReturn(USER);
    }

    @Test
    void createTransaction_validRequest_recordsForAuthenticatedUser() {
        UUID key = UUID.randomUUID();
        var request = new CreateTransactionRequest(BigDecimal.TEN, "EUR", null, "DE89370400440532013000");
        controller.createTransaction(principal, key, request);
        verify(service).record(USER, key, request);
    }

    @Test
    void getTransactions_noFilters_returnsHistoryOfAuthenticatedUser() {
        Pageable page = Pageable.ofSize(20);
        controller.getTransactions(principal, null, null, page);
        verify(service).history(USER, null, null, page);
    }
}
