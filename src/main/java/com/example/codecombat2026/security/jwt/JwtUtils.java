package com.example.codecombat2026.security.jwt;

import com.example.codecombat2026.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${codecombat.jwt.secret}")
    private String jwtSecret;

    @Value("${codecombat.jwt.expiration}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())                  // jti — unique per token, used for blacklist
                .subject(userPrincipal.getUsername())
                .issuedAt(now)                                      // iat — used for "invalidate-before" checks
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    /** Returns the JWT id (jti) claim, or null if not present / parse fails. */
    public String getJtiFromJwtToken(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the iat claim (issued-at) as epoch millis, or 0 if unavailable. */
    public long getIssuedAtMillis(String token) {
        try {
            Date iat = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload().getIssuedAt();
            return iat != null ? iat.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /** Returns the exp claim as epoch millis, or 0 if unavailable. */
    public long getExpiryFromJwtToken(String token) {
        try {
            Date exp = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload().getExpiration();
            return exp != null ? exp.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("JWT signature mismatch: {}", e.getMessage());
        }

        return false;
    }
}
