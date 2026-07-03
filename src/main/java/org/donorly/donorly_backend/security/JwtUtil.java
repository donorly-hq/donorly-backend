package org.donorly.donorly_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long EXPIRY_MS = 1000 * 60 * 60 * 24; // 24 hours

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String role, String ambassadorId) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("ambassadorId", ambassadorId)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    public String extractAmbassadorId(String token) {
        return parseToken(token).get("ambassadorId", String.class);
    }
}
