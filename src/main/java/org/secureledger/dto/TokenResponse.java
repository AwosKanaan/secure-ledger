package org.secureledger.dto;

import org.secureledger.security.JwtService.IssuedToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(IssuedToken token) {
        return new TokenResponse(token.value(), "Bearer", token.ttl().toSeconds());
    }
}
