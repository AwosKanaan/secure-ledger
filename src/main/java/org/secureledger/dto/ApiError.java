package org.secureledger.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String code, String message, List<Violation> errors) {

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(status.value(), null, message, null);
    }

    public record Violation(String field, String message) {
    }
}
