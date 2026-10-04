package org.secureledger.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IbanValidatorTest {

    private final IbanValidator validator = new IbanValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "DE89370400440532013000",
            "GB82WEST12345698765432",
            "FR1420041010050500013M02606",
            "NL91ABNA0417164300",
            "ES9121000418450200051332"})
    void isValid_validIban_returnsTrue(String iban) {
        assertThat(validator.isValid(iban, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DE89370400440532013001",      // one digit changed, wrong checksum
            "DE98370400440532013000",      // check digits transposed
            "GB82WEST1234569876543Z",      // character substitution
            "ZZ89370400440532013000",      // not an ISO 3166 country
            "DE8937040044",                // too short
            "de89370400440532013000",      // lowercase
            "DE89 3704 0044 0532 0130 00", // spaces are not removed by the validator
            "US34123456789012",            // valid checksum, US has no IBANs
            "DE5137040044053201300",       // valid checksum, DE needs 22 characters
            "DE89370400440532013000' OR '1'='1"})
    void isValid_invalidIban_returnsFalse(String iban) {
        assertThat(validator.isValid(iban, null)).isFalse();
    }
}
