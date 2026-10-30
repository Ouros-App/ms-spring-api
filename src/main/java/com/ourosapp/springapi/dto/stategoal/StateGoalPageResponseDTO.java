package com.ourosapp.springapi.dto.stategoal;

import com.ourosapp.springapi.dto.common.PageResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Página de metas e contagens por status do conjunto filtrado completo")
public record StateGoalPageResponseDTO(
        @Schema(description = "Metas da página atual") List<StateGoalResponseDTO> content,
        @Schema(description = "Total de metas no escopo filtrado") long totalElements,
        @Schema(description = "Total de páginas") long totalPages,
        @Schema(description = "Página iniciada em zero") int page,
        @Schema(description = "Tamanho solicitado, entre 1 e 100") int size,
        @Schema(description = "Quantidade de metas por status, independente da página") Map<String, Long> statusCounts) {
    public StateGoalPageResponseDTO { content = List.copyOf(content); statusCounts = Map.copyOf(statusCounts); }
    public static StateGoalPageResponseDTO from(PageResponseDTO<StateGoalResponseDTO> page, Map<String, Long> counts) {
        return new StateGoalPageResponseDTO(page.content(), page.totalElements(), page.totalPages(), page.page(), page.size(), counts);
    }
}
