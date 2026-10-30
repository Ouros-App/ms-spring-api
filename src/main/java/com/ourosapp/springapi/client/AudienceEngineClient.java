package com.ourosapp.springapi.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AudienceEngineClient {

    private final RestClient restClient;

    @Autowired
    public AudienceEngineClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.audience-engine.base-url}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    AudienceEngineClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Busca os IDs de dicas das audiências às quais o usuário pertence.
     *
     * @param userId identificador persistido do usuário
     * @param authorizationHeader token Bearer do usuário, usado pelo Audience Engine para autorizar a consulta
     * @return conjunto de IDs de dicas vinculadas às audiências do usuário
     * @throws RestClientException se a consulta falhar, a resposta violar o contrato ou o snapshot estiver desatualizado
     */
    public Set<Long> getTipIdsForUser(long userId, String authorizationHeader) {
        UserAudiencesResponse response = restClient.get()
                .uri("/internal/v1/users/{userId}/audiences", userId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(UserAudiencesResponse.class);

        if (response == null || response.audiences() == null) {
            throw new RestClientException("Resposta inválida do Audience Engine");
        }
        if (!"CURRENT".equals(response.sourceStatus())) {
            throw new RestClientException("Audience Engine retornou snapshots desatualizados ou inválidos");
        }

        Set<Long> tipIds = new HashSet<>();
        for (AudienceMembership audience : response.audiences()) {
            if (audience == null || audience.tipIds() == null) {
                throw new RestClientException("Resposta inválida do Audience Engine: audiência sem tip_ids");
            }
            for (Long tipId : audience.tipIds()) {
                if (tipId == null) {
                    throw new RestClientException("Resposta inválida do Audience Engine: tip_id nulo");
                }
                tipIds.add(tipId);
            }
        }
        return Set.copyOf(tipIds);
    }

    private record UserAudiencesResponse(
            @JsonProperty("source_status") String sourceStatus,
            List<AudienceMembership> audiences
    ) {}

    private record AudienceMembership(@JsonProperty("tip_ids") List<Long> tipIds) {}
}
