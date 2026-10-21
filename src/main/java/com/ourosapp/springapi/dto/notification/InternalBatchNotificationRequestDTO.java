package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de requisição para ingestão de notificações em lote via endpoint interno.
 */
@Schema(description = "Requisição para ingestão em lote de notificações")
public record InternalBatchNotificationRequestDTO(

        @Schema(description = "Lista contendo entre 1 e 500 notificações a serem processadas")
        @NotNull(message = "A lista de notificações é obrigatória")
        @NotEmpty(message = "A lista de notificações não pode ser vazia")
        @Size(max = 500, message = "O lote não pode exceder 500 notificações por requisição")
        @Valid
        @JsonProperty("notifications")
        List<InternalBatchNotificationItemDTO> notifications
) {

    public InternalBatchNotificationRequestDTO {
        notifications = notifications != null ? List.copyOf(notifications) : List.of();
    }
}
