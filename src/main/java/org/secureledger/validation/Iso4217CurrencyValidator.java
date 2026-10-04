package org.secureledger.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public class Iso4217CurrencyValidator implements ConstraintValidator<Iso4217Currency, String> {

    private static final Set<String> NON_BOOKABLE = Set.of("XXX", "XTS");

    private static final Set<String> CODES = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .filter(code -> !NON_BOOKABLE.contains(code))
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || CODES.contains(value);
    }
}
