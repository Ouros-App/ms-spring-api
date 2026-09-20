package com.ourosapp.springapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simples em memória para os endpoints públicos de cadastro.
 *
 * <p>O limite é aplicado por endereço IP e compartilhado entre os dois endpoints
 * de cadastro. Em ambientes com múltiplas instâncias, cada instância mantém sua
 * própria janela de contagem.</p>
 */
public class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private static final String FARM_OWNER_REGISTRATION = "/farm-owners";
    private static final String COMPANY_EMPLOYEE_REGISTRATION = "/company-employees";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int maxRequests;
    private final long windowSeconds;
    private final boolean trustProxyHeaders;
    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();

    public RegistrationRateLimitFilter(int maxRequests, long windowSeconds) {
        this(maxRequests, windowSeconds, false);
    }

    public RegistrationRateLimitFilter(
            int maxRequests,
            long windowSeconds,
            boolean trustProxyHeaders
    ) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests deve ser maior que zero");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds deve ser maior que zero");
        }

        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.trustProxyHeaders = trustProxyHeaders;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return !FARM_OWNER_REGISTRATION.equals(path)
                && !COMPANY_EMPLOYEE_REGISTRATION.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        String clientIp = resolveClientIp(request);

        cleanupExpiredEntries(now);

        if (!clients.containsKey(clientIp) && clients.size() >= MAX_TRACKED_CLIENTS) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Muitas origens de cadastro ativas. Tente novamente mais tarde.\"}"
            );
            return;
        }

        ClientWindow window = clients.computeIfAbsent(
                clientIp,
                ignored -> new ClientWindow(now, windowSeconds)
        );

        RateLimitDecision decision = window.tryAcquire(now, maxRequests, windowSeconds);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Muitas tentativas de cadastro. Tente novamente mais tarde.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String cloudflareIp = request.getHeader("CF-Connecting-IP");
        if (trustProxyHeaders
                && cloudflareIp != null
                && !cloudflareIp.isBlank()
                && cloudflareIp.length() <= 64) {
            return cloudflareIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }

    private void cleanupExpiredEntries(long now) {
        if (clients.size() < MAX_TRACKED_CLIENTS) {
            return;
        }

        clients.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowSeconds));
    }

    private static final class ClientWindow {

        private long startedAt;
        private int requests;

        private ClientWindow(long startedAt, long windowSeconds) {
            this.startedAt = alignToWindow(startedAt, windowSeconds);
            this.requests = 0;
        }

        private synchronized RateLimitDecision tryAcquire(
                long now,
                int maxRequests,
                long windowSeconds
        ) {
            long currentWindow = alignToWindow(now, windowSeconds);
            if (currentWindow != startedAt) {
                startedAt = currentWindow;
                requests = 0;
            }

            long retryAfter = Math.max(1, startedAt + windowSeconds - now);

            if (requests >= maxRequests) {
                return new RateLimitDecision(false, 0, retryAfter);
            }

            requests++;
            return new RateLimitDecision(true, maxRequests - requests, retryAfter);
        }

        private synchronized boolean isExpired(long now, long windowSeconds) {
            return now >= startedAt + (windowSeconds * 2);
        }

        private static long alignToWindow(long epochSecond, long windowSeconds) {
            return epochSecond - (epochSecond % windowSeconds);
        }
    }

    private record RateLimitDecision(
            boolean allowed,
            int remaining,
            long retryAfterSeconds
    ) {
    }
}
