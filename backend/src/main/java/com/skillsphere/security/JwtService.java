package com.skillsphere.security;

import com.skillsphere.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and validates the application's short-lived JSON Web Tokens.
 *
 * A JWT is signed with an HMAC secret. On a later request JJWT verifies that signature before any
 * claim is trusted, then also checks expiration. JWT avoids server-side session storage, which is
 * why it pairs naturally with SessionCreationPolicy.STATELESS. The trade-off is that this simple
 * project intentionally has no refresh tokens or token blacklist.
 */
@Service
public class JwtService {

    private final String jwtSecret;
    private final long expirationMilliseconds;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMilliseconds
    ) {
        this.jwtSecret = jwtSecret;
        this.expirationMilliseconds = expirationMilliseconds;
    }

    /** Generates a 30-minute token containing the interview-relevant account claims. */
    public String generateToken(User user) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMilliseconds);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses the signed token. JJWT throws a JwtException for a bad signature, malformed token,
     * or expired token, allowing the filter to return one consistent 401 response.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public long getExpirationSeconds() {
        return expirationMilliseconds / 1_000;
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
