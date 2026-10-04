package org.secureledger.service;

import lombok.RequiredArgsConstructor;
import org.secureledger.dao.TransactionDAO;
import org.secureledger.dao.UserDAO;
import org.secureledger.dto.CreateTransactionRequest;
import org.secureledger.dto.PageResponse;
import org.secureledger.dto.TransactionResponse;
import org.secureledger.exception.IdempotencyKeyReusedException;
import org.secureledger.model.Transaction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Sort DEFAULT_ORDER = Sort.by(Sort.Order.desc("createdAt"));
    private static final List<String> SORTABLE_PROPERTIES = List.of("createdAt", "amount");
    private static final Instant OPEN_START = Instant.EPOCH;
    private static final Instant OPEN_END = Instant.parse("9999-12-31T00:00:00Z");

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;
    private final Clock clock;

    @Transactional
    public TransactionResponse record(UUID userId, UUID idempotencyKey, CreateTransactionRequest request) {
        BigDecimal amount = request.amount().setScale(4, RoundingMode.UNNECESSARY);

        if (idempotencyKey != null) {
            Optional<Transaction> previous = transactionDAO.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (previous.isPresent()) {
                if (!isSamePayload(previous.get(), amount, request)) {
                    throw new IdempotencyKeyReusedException();
                }
                return TransactionResponse.from(previous.get());
            }
        }

        Transaction transaction = new Transaction(
                userDAO.getReferenceById(userId),
                idempotencyKey != null ? idempotencyKey : UUID.randomUUID(),
                amount,
                request.currency(),
                request.description(),
                request.counterpartyIban(),
                clock.instant().truncatedTo(ChronoUnit.MICROS));
        return TransactionResponse.from(transactionDAO.saveAndFlush(transaction));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(UUID userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be on or before endDate");
        }

        Instant from = startDate == null ? OPEN_START : startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = endDate == null ? OPEN_END : endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), historySort(pageable.getSort()));
        return PageResponse.from(
                transactionDAO.findUserTransactionHistory(userId, from, to, page).map(TransactionResponse::from));
    }

    private static Sort historySort(Sort requested) {
        Sort sort = requested.isSorted() ? requested : DEFAULT_ORDER;
        for (Sort.Order order : sort) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sorting is supported on: " + String.join(", ", SORTABLE_PROPERTIES));
            }
        }
        return sort.and(Sort.by(Sort.Order.desc("id")));
    }

    private static boolean isSamePayload(Transaction stored, BigDecimal amount, CreateTransactionRequest request) {
        return stored.getAmount().compareTo(amount) == 0
                && stored.getCurrency().equals(request.currency())
                && stored.getCounterpartyIban().equals(request.counterpartyIban())
                && Objects.equals(stored.getDescription(), request.description());
    }
}
