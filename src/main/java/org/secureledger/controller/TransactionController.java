package org.secureledger.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.secureledger.config.constants.ApiHeaders;
import org.secureledger.dto.CreateTransactionRequest;
import org.secureledger.dto.PageResponse;
import org.secureledger.dto.TransactionResponse;
import org.secureledger.security.AuthenticatedUser;
import org.secureledger.service.TransactionService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/ledger/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions")
class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a transaction for the authenticated user",
            description = "Idempotency-Key (a UUID) is optional. Supply one per transaction and reuse it on retries: "
                    + "a repeated request returns the original transaction instead of booking it twice. "
                    + "Reusing a key for a different payload returns 422. "
                    + "When the header is omitted the server generates a key and the request is booked as new.")
    TransactionResponse createTransaction(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestHeader(value = ApiHeaders.IDEMPOTENCY_KEY, required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.record(principal.id(), idempotencyKey, request);
    }

    @GetMapping
    @Operation(summary = "Paginated transaction history of the authenticated user",
            description = "startDate and endDate are optional, inclusive ISO dates (yyyy-MM-dd, UTC). "
                    + "sort accepts createdAt or amount, e.g. sort=amount,asc (default createdAt,desc). "
                    + "page starts at 1. Page size defaults to 10 and is capped at 100.")
    PageResponse<TransactionResponse> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @ParameterObject Pageable pageable) {

        return transactionService.history(principal.id(), startDate, endDate, pageable);
    }
}
