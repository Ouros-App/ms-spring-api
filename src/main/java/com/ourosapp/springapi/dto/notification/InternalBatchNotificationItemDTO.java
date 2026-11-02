package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO que representa um item individual dentro de um lote de notificações internas.
 */
@Schema(description = "Item de notificação para ingestão e processamento em lote")
public record InternalBatchNotificationItemDTO(

        @Schema(description = "Identificador único obrigatório do evento para idempotência", example = "aud:high_water_consumption:v1:user:10:farm:5")
        @NotBlank(message = "O identificador do evento é obrigatório")
        @Size(max = 255, message = "O identificador do evento não pode exceder 255 caracteres")
        @JsonProperty("event_id")
        @JsonAlias("eventId")
        String eventId,

        @Schema(description = "Identificador numérico do usuário destinatário no banco de dados relacional", example = "10")
        @NotNull(message = "O identificador do usuário é obrigatório")
        @JsonProperty("user_id")
        @JsonAlias("userId")
        Long userId,

        @Schema(description = "Perfil de acesso do usuário destinatário (ADM, COMPANY_EMPLOYEE, FARM_OWNER)", example = "FARM_OWNER")
        @NotBlank(message = "O perfil do usuário é obrigatório")
        @Pattern(regexp = "^(ADM|COMPANY_EMPLOYEE|FARM_OWNER)$", message = "O perfil do usuário deve ser ADM, COMPANY_EMPLOYEE ou FARM_OWNER")
        @JsonProperty("user_role")
        @JsonAlias("userRole")
        String userRole,

        @Schema(description = "Título da notificação exibido no banner ou lista in-app", example = "Alerta de Consumo de Água")
        @NotBlank(message = "O título da notificação é obrigatório")
        @Size(max = 150, message = "O título da notificação não pode exceder 150 caracteres")
        @JsonProperty("title")
        String title,

        @Schema(description = "Mensagem ou corpo textual detalhado da notificação", example = "Detectamos consumo acima da média histórica na sua granja. Confira as dicas técnicas.")
        @NotBlank(message = "A mensagem da notificação é obrigatória")
        @JsonProperty("message")
        String message,

        @Schema(description = "Tipo ou categoria do evento de notificação", example = "TIP_ACTIVATED", defaultValue = "TIP_ACTIVATED")
        @JsonProperty("type")
        String type,

        @Schema(description = "Identificador opcional da dica técnica vinculada ao evento", example = "3")
        @JsonProperty("id_tip")
        @JsonAlias("idTip")
        Long idTip,

        @Schema(description = "Payload adicional serializado em formato JSON com metadados do evento", example = "{\"farm_id\": 5, \"metric\": \"water\", \"increase_pct\": 28.5}")
        @JsonProperty("data_payload")
        @JsonAlias("dataPayload")
        String dataPayload,

        @Schema(description = "Indica se deve enfileirar Push Notification via FCM (se falso, apenas persiste in-app)", example = "true", defaultValue = "true")
        @JsonProperty("send_push")
        @JsonAlias("sendPush")
        Boolean sendPush
) {

    public InternalBatchNotificationItemDTO {
        eventId = eventId != null ? eventId.trim() : null;
        if (eventId != null && eventId.isEmpty()) {
            eventId = null;
        }
        userRole = userRole != null ? userRole.trim().toUpperCase() : null;
        title = title != null ? title.trim() : null;
        message = message != null ? message.trim() : null;
        type = type != null && !type.trim().isEmpty() ? type.trim().toUpperCase() : "TIP_ACTIVATED";
        dataPayload = dataPayload != null && !dataPayload.trim().isEmpty() ? dataPayload.trim() : null;
        if (sendPush == null) {
            sendPush = Boolean.TRUE;
        }
    }
}
