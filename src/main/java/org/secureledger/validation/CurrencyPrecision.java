package org.secureledger.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = CurrencyPrecisionValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface CurrencyPrecision {

    String message() default "has more decimal places than the currency allows";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
