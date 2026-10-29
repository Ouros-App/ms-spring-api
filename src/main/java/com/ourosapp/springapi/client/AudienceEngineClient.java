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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Set<Long> getTipIdsForUser(long userId, String authorizationHeader) {
        UserAudiencesResponse response = restClient.get()
                .uri("/internal/v1/users/{userId}/audiences", userId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(UserAudiencesResponse.class);

        if (response == null || response.audiences() == null) {
            throw new RestClientException("Resposta inválida do Audience Engine");
        }

        return response.audiences().stream()
                .map(AudienceMembership::tipIds)
                .peek(tipIds -> {
                    if (tipIds == null) {
                        throw new RestClientException("Audience sem tip_ids na resposta");
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record UserAudiencesResponse(List<AudienceMembership> audiences) {}

    private record AudienceMembership(@JsonProperty("tip_ids") List<Long> tipIds) {}
}
