package org.secureledger.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.secureledger.dto.LoginRequest;
import org.secureledger.dto.TokenResponse;
import org.secureledger.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ledger/auth")
@RequiredArgsConstructor
@SecurityRequirements
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for a JWT access token")
    TokenResponse createToken(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
