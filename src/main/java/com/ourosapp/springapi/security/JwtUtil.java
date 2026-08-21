package com.ourosapp.springapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 1. Gera token com ID, Email e Role
    public String generateToken(Long id, String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // 2. Sobrecarga simplificada caso precise gerar apenas por email e role
    public String generateToken(String email, String role) {
        return generateToken(null, email, role);
    }

    // 3. Extrai o ID de dentro do token
    public Long getIdFromToken(String token) {
        Object idClaim = getClaims(token).get("id");
        if (idClaim instanceof Integer) {
            return ((Integer) idClaim).longValue();
        }
        return (Long) idClaim;
    }

    // 4. Extrai o email
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 5. Extrai a role
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 6. Valida o token
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
