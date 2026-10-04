package org.secureledger.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.secureledger.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(30)
                .build();
    }

    public IssuedToken issue(UUID userId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.ttl());
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, properties.ttl());
    }

    public UUID verifyAndExtractUserId(String token) {
        String subject = parser.parseSignedClaims(token).getPayload().getSubject();
        if (subject == null) {
            throw new MalformedJwtException("Token has no subject");
        }
        return UUID.fromString(subject);
    }

    public record IssuedToken(String value, Duration ttl) {
    }
}
