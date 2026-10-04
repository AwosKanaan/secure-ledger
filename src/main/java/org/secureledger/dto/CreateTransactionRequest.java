package org.secureledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.secureledger.validation.CurrencyPrecision;
import org.secureledger.validation.Iban;
import org.secureledger.validation.Iso4217Currency;

import java.math.BigDecimal;
import java.util.Locale;

@CurrencyPrecision
public record CreateTransactionRequest(
        @Schema(type = "string", example = "125.50",
                description = "Positive decimal. Send as a JSON string to avoid binary floating-point loss on the client.")
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @Schema(example = "EUR")
        @NotNull
        @Iso4217Currency
        String currency,

        @Schema(example = "Invoice 2026-0042")
        @Size(max = 140)
        @Pattern(regexp = "^\\P{Cntrl}*$", message = "must not contain control characters")
        String description,

        @Schema(example = "DE89 3704 0044 0532 0130 00", description = "Whitespace is allowed and stripped")
        @NotNull
        @Iban
        String counterpartyIban) {

    public CreateTransactionRequest {
        currency = currency == null ? null : currency.strip().toUpperCase(Locale.ROOT);
        counterpartyIban = counterpartyIban == null ? null : counterpartyIban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        description = description == null || description.isBlank() ? null : description.strip();
    }
}
