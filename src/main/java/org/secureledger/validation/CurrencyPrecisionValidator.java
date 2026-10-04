package org.secureledger.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.secureledger.dto.CreateTransactionRequest;

import java.util.Currency;

public class CurrencyPrecisionValidator implements ConstraintValidator<CurrencyPrecision, CreateTransactionRequest> {

    @Override
    public boolean isValid(CreateTransactionRequest value, ConstraintValidatorContext context) {
        if (value == null || value.amount() == null || value.currency() == null) {
            return true;
        }
        int minorUnits;
        try {
            minorUnits = Currency.getInstance(value.currency()).getDefaultFractionDigits();
        } catch (IllegalArgumentException unknownCurrency) {
            return true;
        }
        if (minorUnits < 0 || value.amount().stripTrailingZeros().scale() <= minorUnits) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "must have at most " + minorUnits + " decimal place(s) for " + value.currency())
                .addPropertyNode("amount")
                .addConstraintViolation();
        return false;
    }
}
