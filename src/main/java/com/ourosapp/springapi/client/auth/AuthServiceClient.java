package com.ourosapp.springapi.client.auth;

import com.ourosapp.springapi.client.auth.dto.AuthVerifyRequestDTO;
import com.ourosapp.springapi.client.auth.dto.AuthVerifyResponseDTO;
import com.ourosapp.springapi.client.auth.exception.AuthRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Cliente HTTP para comunicação com o serviço centralizado de autenticação ms-auth-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestClient authServiceRestClient;

    /**
     * Valida as credenciais junto ao microsserviço ms-auth-service.
     *
     * @param email       e-mail do usuário
     * @param password    senha informada no formulário de login
     * @param accountType tipo de conta esperado ("admin", "company_employee", "farm_owner") ou null
     * @return Optional contendo a identidade validada se o login for bem-sucedido, ou vazio se credenciais inválidas
     * @throws AuthRateLimitException se a taxa limite de tentativas for excedida (HTTP 429)
     * @throws ResponseStatusException HTTP 409 se credenciais forem ambíguas, ou HTTP 503 se o serviço estiver indisponível
     */
    public Optional<AuthVerifyResponseDTO.IdentityDTO> verifyCredentials(String email, String password, String accountType) {
        AuthVerifyRequestDTO requestPayload = new AuthVerifyRequestDTO(email, password, accountType);

        try {
            AuthVerifyResponseDTO response = authServiceRestClient.post()
                    .uri("/v1/auth/credentials/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(AuthVerifyResponseDTO.class);

            if (response != null && response.authenticated() && response.identity() != null) {
                return Optional.of(response.identity());
            }
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.debug("ms-auth-service retornou 401 Unauthorized para o e-mail: {}", email);
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("ms-auth-service retornou 429 Too Many Requests para o e-mail: {}", email);
            Long retryAfterSeconds = parseRetryAfter(ex.getResponseHeaders());
            throw new AuthRateLimitException("Muitas tentativas de login. Tente novamente mais tarde.", retryAfterSeconds);
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("ms-auth-service retornou 409 Conflict para o e-mail: {}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As credenciais correspondem a mais de uma conta. Especifique o tipo de conta.");
        } catch (HttpClientErrorException ex) {
            log.error("Erro cliente ao consultar ms-auth-service: status={}", ex.getStatusCode(), ex);
            throw new ResponseStatusException(ex.getStatusCode(), "Erro na validação de credenciais.");
        } catch (HttpServerErrorException ex) {
            log.error("ms-auth-service retornou erro de servidor: status={}", ex.getStatusCode(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação temporariamente indisponível.");
        } catch (ResourceAccessException ex) {
            log.error("Falha de conexão ou timeout com ms-auth-service", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação temporariamente indisponível.");
        } catch (Exception ex) {
            log.error("Erro inesperado ao consultar ms-auth-service", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno de autenticação.");
        }
    }

    private Long parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter.trim());
            } catch (NumberFormatException ignored) {
                // Ignora falha de formatação e retorna null
            }
        }
        return null;
    }
}
