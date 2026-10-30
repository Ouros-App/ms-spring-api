package com.ourosapp.springapi.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Página de resultados; os totais consideram todos os filtros e o escopo autorizado")
public record PageResponseDTO<T>(
        @Schema(description = "Itens desta página") List<T> content,
        @Schema(description = "Total de itens filtrados") long totalElements,
        @Schema(description = "Total de páginas") long totalPages,
        @Schema(description = "Índice da página iniciado em zero") int page,
        @Schema(description = "Tamanho solicitado, entre 1 e 100") int size) {
    public PageResponseDTO { content = List.copyOf(content); }
    public static <T> PageResponseDTO<T> from(Page<T> result) {
        return of(result.getContent(), result.getTotalElements(), result.getNumber(), result.getSize());
    }
    public static <T> PageResponseDTO<T> of(List<T> content, long total, int page, int size) {
        return new PageResponseDTO<>(content, total, total / size + (total % size == 0 ? 0 : 1), page, size);
    }
}
