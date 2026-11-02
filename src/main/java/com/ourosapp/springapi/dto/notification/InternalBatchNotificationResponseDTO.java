package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de resposta contendo o resultado da ingestão e processamento de notificações em lote.
 */
@Schema(description = "Métricas e sumário do processamento em lote de notificações")
public record InternalBatchNotificationResponseDTO(

        @Schema(description = "Total de notificações recebidas na requisição", example = "10")
        @JsonProperty("total_received")
        int totalReceived,

        @Schema(description = "Total de notificações persistidas com sucesso", example = "8")
        @JsonProperty("total_created")
        int totalCreated,

        @Schema(description = "Total de notificações ignoradas por deduplicação (event_id já existente)", example = "2")
        @JsonProperty("total_skipped")
        int totalSkipped,

        @Schema(description = "Total de notificações com push enfileirado para entrega assíncrona", example = "8")
        @JsonProperty("total_push_queued")
        int totalPushQueued,

        @Schema(description = "Data e hora de conclusão do processamento do lote", example = "2026-10-21T11:45:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("processed_at")
        LocalDateTime processedAt
) {
}
