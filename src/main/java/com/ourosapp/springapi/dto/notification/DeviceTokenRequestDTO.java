package com.ourosapp.springapi.dto.notification;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para registro ou atualização de token de dispositivo FCM.
 *
 * @param token token do Firebase Cloud Messaging (FCM)
 * @param platform plataforma do dispositivo (ANDROID, IOS, WEB)
 */
@Schema(description = "Dados para cadastro ou atualização de token de dispositivo para Push Notifications")
public record DeviceTokenRequestDTO(

        @Schema(description = "Token de registro gerado pelo Firebase Cloud Messaging (FCM)", example = "fcm_token_sample_abc123...")
        @NotBlank(message = "O token do dispositivo não pode estar em branco")
        @Size(max = 500, message = "O token do dispositivo deve ter no máximo 500 caracteres")
        @JsonProperty("token")
        String token,

        @Schema(description = "Plataforma do dispositivo móvel", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
        @NotBlank(message = "A plataforma do dispositivo não pode estar em branco")
        @Pattern(regexp = "^(?i)(ANDROID|IOS|WEB)$", message = "A plataforma deve ser ANDROID, IOS ou WEB")
        @JsonProperty("platform")
        @JsonAlias("platform")
        String platform
) {
    public DeviceTokenRequestDTO {
        token = token != null ? token.trim() : null;
        platform = platform != null ? platform.trim().toUpperCase() : null;
    }
}
