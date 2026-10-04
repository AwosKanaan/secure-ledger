package org.secureledger.service;

import lombok.RequiredArgsConstructor;
import org.secureledger.dto.LoginRequest;
import org.secureledger.dto.TokenResponse;
import org.secureledger.security.AuthenticatedUser;
import org.secureledger.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return TokenResponse.bearer(jwtService.issue(user.id()));
    }
}
