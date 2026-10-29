package com.ourosapp.springapi.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AudienceEngineClientTest {

    @Test
    void deveEncaminharJwtEExtrairIdsDasDicasDasAudiencias() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AudienceEngineClient client = new AudienceEngineClient(
                builder.baseUrl("https://audience.test").build()
        );

        server.expect(requestTo("https://audience.test/internal/v1/users/7/audiences"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andRespond(withSuccess("""
                        {
                          "user_id": 7,
                          "audiences": [
                            {"key": "water-audit", "version": "1", "tip_ids": [121, 122], "snapshot_version": 4}
                          ],
                          "source_status": "CURRENT"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Set.of(121L, 122L), client.getTipIdsForUser(7L, "Bearer user-token"));
        server.verify();
    }
}
