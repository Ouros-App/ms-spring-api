package com.ourosapp.springapi.service;

import com.ourosapp.springapi.client.AudienceEngineClient;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.RestClientException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipAudienceEnricherTest {

    @Mock
    private AudienceEngineClient audienceEngineClient;

    @InjectMocks
    private TipAudienceEnricher enricher;

    @Test
    void deveMarcarQuaisDicasCorrespondemAsAudienciasDoProdutor() {
        UserPrincipal owner = new UserPrincipal(
                7L,
                "owner@example.com",
                "unused",
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
        TipResponseDTO matchedTip = tip(121L);
        TipResponseDTO unmatchedTip = tip(122L);
        when(audienceEngineClient.getTipIdsForUser(7L, "Bearer user-token"))
                .thenReturn(Set.of(121L));

        List<TipResponseDTO> result = enricher.enrich(
                List.of(matchedTip, unmatchedTip), owner, "Bearer user-token"
        );

        assertEquals(Boolean.TRUE, result.get(0).audienceMatch());
        assertEquals(Boolean.FALSE, result.get(1).audienceMatch());
        verify(audienceEngineClient).getTipIdsForUser(7L, "Bearer user-token");
    }

    @Test
    void deveManterDicasSemMatchQuandoProdutorNaoPertenceAAlgumaAudiencia() {
        UserPrincipal owner = farmOwner();
        when(audienceEngineClient.getTipIdsForUser(7L, "Bearer user-token")).thenReturn(Set.of());

        List<TipResponseDTO> result = enricher.enrich(
                List.of(tip(121L)), owner, "Bearer user-token"
        );

        assertFalse(result.get(0).audienceMatch());
    }

    @Test
    void devePreservarDicasSemAudienceMatchQuandoAudienceEstaIndisponivel() {
        UserPrincipal owner = farmOwner();
        when(audienceEngineClient.getTipIdsForUser(7L, "Bearer user-token"))
                .thenThrow(new RestClientException("unavailable"));
        TipResponseDTO tip = tip(121L);

        List<TipResponseDTO> result = enricher.enrich(List.of(tip), owner, "Bearer user-token");

        assertSame(tip, result.get(0));
        assertNull(result.get(0).audienceMatch());
    }

    @Test
    void naoConsultaAudienceParaPerfisSemAudienciasDeProdutor() {
        UserPrincipal admin = new UserPrincipal(
                1L,
                "admin@example.com",
                "unused",
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        List<TipResponseDTO> tips = List.of(tip(121L));

        assertSame(tips, enricher.enrich(tips, admin, "Bearer user-token"));
        verifyNoInteractions(audienceEngineClient);
    }

    private static TipResponseDTO tip(Long id) {
        return new TipResponseDTO(id, "title", "text", null, 1L, List.of(), 0, 0.0);
    }

    private static UserPrincipal farmOwner() {
        return new UserPrincipal(
                7L,
                "owner@example.com",
                "unused",
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }
}
