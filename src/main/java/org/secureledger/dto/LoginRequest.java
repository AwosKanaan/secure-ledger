package org.secureledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "gilbert@gmail.com")
        @NotBlank String email,

        @Schema(example = "gilbert123")
        @NotBlank
        @Size(max = 100) String password) {
}
