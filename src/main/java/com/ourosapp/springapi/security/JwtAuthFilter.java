package com.ourosapp.springapi.security;

import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP executado uma vez por requisição para autenticar tokens JWT via cabeçalho Authorization Bearer.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Processa cada requisição HTTP verificando a presença de token JWT no cabeçalho Authorization.
     * Se válido, autentica o usuário no contexto do Spring Security.
     *
     * @param request     a requisição HTTP
     * @param response    a resposta HTTP
     * @param filterChain a cadeia de filtros a ser executada
     * @throws ServletException em caso de erro no processamento do filtro
     * @throws IOException      em caso de erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = authHeader.substring(7).trim();

                if (!token.isEmpty()) {
                    try {
                        Claims claims = jwtUtil.extractAllClaims(token);
                        String email = claims.getSubject();
                        String role = claims.get("role", String.class);

                        UserDetails userDetails = userDetailsService.loadUserByEmailAndRole(email, role);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } catch (Exception ex) {
                        // Token inválido, malformado, expirado ou usuário não encontrado:
                        // limpa o contexto e segue a cadeia para que o Spring Security avalie o acesso à rota
                        SecurityContextHolder.clearContext();
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
