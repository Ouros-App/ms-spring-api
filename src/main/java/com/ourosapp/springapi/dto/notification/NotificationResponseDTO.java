package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO de resposta contendo os detalhes de uma Notificação In-App.
 *
 * @param id identificador único da notificação
 * @param title título da notificação
 * @param message mensagem descritiva
 * @param type tipo da notificação (ex: RANKING_UPDATED)
 * @param dataPayload metadados adicionais em formato JSON
 * @param isRead indicador se a notificação foi lida
 * @param createdAt data e hora de recebimento
 * @param readAt data e hora de leitura (se lida)
 */
@Schema(description = "Resposta contendo os dados da notificação")
public record NotificationResponseDTO(

        @Schema(description = "Identificador único da notificação", example = "1")
        Long id,

        @Schema(description = "Título da notificação", example = "Ranking Atualizado!")
        String title,

        @Schema(description = "Mensagem ou conteúdo da notificação", example = "A sua fazenda Boa Vista alcançou o 1º lugar (Medalha de Ouro).")
        String message,

        @Schema(description = "Tipo do evento de notificação", example = "RANKING_UPDATED")
        String type,

        @Schema(description = "Payload JSON com dados adicionais do evento", example = "{\"farm_id\":1,\"rank_position\":1,\"medal\":\"OURO\"}")
        @JsonProperty("data_payload")
        @JsonAlias("dataPayload")
        String dataPayload,

        @Schema(description = "Indica se a notificação já foi visualizada/lida pelo usuário", example = "false")
        @JsonProperty("is_read")
        @JsonAlias("isRead")
        boolean isRead,

        @Schema(description = "Data e hora de criação da notificação", example = "2026-10-14T11:00:00")
        @JsonProperty("created_at")
        @JsonAlias("createdAt")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora em que a notificação foi marcada como lida", example = "2026-10-14T11:05:00")
        @JsonProperty("read_at")
        @JsonAlias("readAt")
        LocalDateTime readAt
) {
    public static NotificationResponseDTO fromEntity(Notification notification) {
        Objects.requireNonNull(notification, "Notification não pode ser nula");
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getDataPayload(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
