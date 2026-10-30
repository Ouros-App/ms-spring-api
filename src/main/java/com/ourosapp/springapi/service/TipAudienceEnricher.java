package com.ourosapp.springapi.service;

import com.ourosapp.springapi.client.AudienceEngineClient;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipAudienceEnricher {

    private final AudienceEngineClient audienceEngineClient;

    /**
     * Marca cada dica como pertencente ou não a uma audiência do produtor autenticado.
     *
     * @param tips dicas retornadas pelo serviço de domínio
     * @param principal usuário autenticado
     * @param authorizationHeader token Bearer encaminhado ao Audience Engine
     * @return dicas enriquecidas, ou originais se o usuário não for produtor ou o Engine falhar
     */
    public List<TipResponseDTO> enrich(
            List<TipResponseDTO> tips,
            UserPrincipal principal,
            String authorizationHeader
    ) {
        if (tips.isEmpty()
                || principal == null
                || !FARM_OWNER.equals(principal.getRole())
                || principal.getId() == null
                || !StringUtils.hasText(authorizationHeader)) {
            return tips;
        }

        try {
            Set<Long> audienceTipIds = audienceEngineClient.getTipIdsForUser(
                    principal.getId(), authorizationHeader
            );
            return tips.stream()
                    .map(tip -> tip.withAudienceMatch(audienceTipIds.contains(tip.id())))
                    .toList();
        } catch (RestClientException ex) {
            log.warn(
                    "Audience Engine indisponível ou snapshot desatualizado; retornando dicas sem audience_match para user_id={}",
                    principal.getId(),
                    ex
            );
            return tips;
        }
    }
}
