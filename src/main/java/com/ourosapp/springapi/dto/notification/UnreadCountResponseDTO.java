package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta para a contagem de notificações não lidas.
 *
 * @param unreadCount total de notificações não lidas
 */
@Schema(description = "Resposta contendo o total de notificações não lidas")
public record UnreadCountResponseDTO(

        @Schema(description = "Total de notificações pendentes de leitura", example = "5")
        @JsonProperty("unread_count")
        @JsonAlias("unreadCount")
        long unreadCount
) {
}
