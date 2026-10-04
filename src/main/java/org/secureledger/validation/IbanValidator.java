package org.secureledger.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.iban4j.Iban4jException;
import org.iban4j.IbanUtil;

public class IbanValidator implements ConstraintValidator<Iban, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            IbanUtil.validate(value);
            return true;
        } catch (Iban4jException invalidIban) {
            return false;
        }
    }
}
