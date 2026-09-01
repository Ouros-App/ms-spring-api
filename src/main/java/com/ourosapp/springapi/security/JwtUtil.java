package com.ourosapp.springapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitário responsável pela geração, validação e extração de claims de tokens JWT (JSON Web Tokens).
 * Utiliza o algoritmo de assinatura HS256 (HMAC-SHA256) e valida a chave secreta na inicialização.
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey signingKey;

    /**
     * Valida a presença e o tamanho mínimo da chave secreta JWT e inicializa a chave de assinatura.
     * Executado automaticamente após a construção do bean Spring.
     *
     * @throws IllegalStateException se a chave secreta for nula ou tiver menos de 32 caracteres
     */
    @PostConstruct
    public void validateAndInitSecret() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("A chave secreta 'app.jwt.secret' deve conter no mínimo 32 caracteres (256 bits).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Retorna a chave de assinatura HMAC usada para gerar e validar tokens JWT.
     *
     * @return a chave secreta de assinatura
     */
    private SecretKey getSigningKey() {
        return this.signingKey;
    }

    /**
     * Gera token JWT assinado contendo ID, e-mail (subject) e role como claims.
     *
     * @param id    o ID do usuário
     * @param email o e-mail do usuário
     * @param role  o perfil de autorização
     * @return token JWT formatado
     */
    public String generateToken(Long id, String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Sobrecarga para gerar token com e-mail e role.
     *
     * @param email o e-mail do usuário
     * @param role  o perfil de autorização
     * @return token JWT formatado
     */
    public String generateToken(String email, String role) {
        return generateToken(null, email, role);
    }

    /**
     * Valida a assinatura e extrai todas as claims do token JWT em uma única operação criptográfica.
     *
     * @param token o token JWT
     * @return o conjunto de Claims contido no payload
     * @throws JwtException             se o token for inválido, malformado ou expirado
     * @throws IllegalArgumentException se o token for nulo ou vazio
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrai o ID do usuário de dentro do token JWT.
     *
     * @param token o token JWT
     * @return o ID do usuário ou null
     */
    public Long getIdFromToken(String token) {
        Object idClaim = extractAllClaims(token).get("id");
        return (idClaim instanceof Number number) ? number.longValue() : null;
    }

    /**
     * Extrai o e-mail (subject) do token JWT.
     *
     * @param token o token JWT
     * @return o e-mail do usuário
     */
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrai a role do token JWT.
     *
     * @param token o token JWT
     * @return o perfil de autorização
     */
    public String getRoleFromToken(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Valida a integridade e expiração do token JWT.
     *
     * @param token o token JWT
     * @return true se o token for válido; false caso contrário
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
