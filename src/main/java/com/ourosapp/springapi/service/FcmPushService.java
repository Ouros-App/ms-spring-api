package com.ourosapp.springapi.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável pelo disparo de Push Notifications via Firebase Cloud Messaging (FCM).
 * Executa de forma assíncrona para não onerar as threads de requisição HTTP ou de jobs agendados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Envia push notification para múltiplos tokens de dispositivos FCM.
     *
     * @param tokens lista de tokens FCM dos aparelhos
     * @param title título da notificação
     * @param body corpo/texto da notificação
     * @param data payload adicional de dados (chave-valor)
     */
    @Async("notificationTaskExecutor")
    public void sendMulticastPush(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase não configurado/inicializado. Notificação push ignorada ({} tokens): '{}'", tokens.size(), title);
            return;
        }

        List<String> distinctTokens = tokens.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .distinct()
                .toList();

        if (distinctTokens.isEmpty()) {
            return;
        }

        int batchSize = 500;
        for (int i = 0; i < distinctTokens.size(); i += batchSize) {
            List<String> subList = distinctTokens.subList(i, Math.min(i + batchSize, distinctTokens.size()));
            sendBatch(subList, title, body, data);
        }
    }

    private void sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            com.google.firebase.messaging.Notification fcmNotification =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build();

            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(fcmNotification);

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            MulticastMessage message = builder.build();
            BatchResponse batchResponse = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            log.info("Push enviado para {} dispositivos. Sucessos: {}, Falhas: {}",
                    tokens.size(), batchResponse.getSuccessCount(), batchResponse.getFailureCount());

            if (batchResponse.getFailureCount() > 0) {
                handleFailures(tokens, batchResponse.getResponses());
            }
        } catch (Exception ex) {
            log.error("Erro ao despachar Push Notifications em lote via FCM: {}", ex.getMessage(), ex);
        }
    }

    private void handleFailures(List<String> tokens, List<SendResponse> responses) {
        List<String> tokensToRemove = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (!response.isSuccessful()) {
                FirebaseMessagingException exception = response.getException();
                if (exception != null && exception.getMessagingErrorCode() != null) {
                    MessagingErrorCode code = exception.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        tokensToRemove.add(tokens.get(i));
                    }
                }
            }
        }

        if (!tokensToRemove.isEmpty()) {
            log.info("Removendo {} tokens FCM inválidos ou não mais registrados...", tokensToRemove.size());
            tokensToRemove.forEach(deviceTokenRepository::deleteByToken);
        }
    }
}
