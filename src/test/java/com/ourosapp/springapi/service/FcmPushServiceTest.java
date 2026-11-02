package com.ourosapp.springapi.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link FcmPushService}.
 */
@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private FcmPushService fcmPushService;

    @Test
    @DisplayName("sendMulticastPushNow - Retorna imediatamente sem tokens")
    void deveRetornarQuandoListaTokensNulaOuVazia() {
        fcmPushService.sendMulticastPushNow(null, "Título", "Corpo", Map.of());
        fcmPushService.sendMulticastPushNow(List.of(), "Título", "Corpo", Map.of());

        verifyNoInteractions(deviceTokenRepository);
    }

    @Test
    @DisplayName("sendMulticastPushNow - Propaga erro quando Firebase não estiver inicializado")
    void devePropagarErroQuandoFirebaseNaoInicializado() {
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> fcmPushService.sendMulticastPushNow(
                            List.of("token_123"), "Título", "Corpo", Map.of("key", "val")
                    )
            );
            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPushNow - Ignora tokens nulos ou em branco")
    void deveIgnorarQuandoTokensForemBrancosOuNulos() {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));

            List<String> blankTokens = new ArrayList<>();
            blankTokens.add(null);
            blankTokens.add("   ");
            blankTokens.add("");

            fcmPushService.sendMulticastPushNow(blankTokens, "Título", "Corpo", null);

            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPushNow - Envia para tokens distintos")
    void deveEnviarPushComSucesso() throws Exception {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        BatchResponse mockBatchResponse = mock(BatchResponse.class);

        when(mockBatchResponse.getSuccessCount()).thenReturn(2);
        when(mockBatchResponse.getFailureCount()).thenReturn(0);
        when(mockMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockBatchResponse);

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> messagingStatic = mockStatic(FirebaseMessaging.class)) {

            appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            List<String> tokens = List.of("token_1", "token_2", "token_1");
            fcmPushService.sendMulticastPushNow(tokens, "Título Teste", "Corpo Teste", Map.of("id", "123"));

            verify(mockMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPushNow - Particiona acima de 500 tokens")
    void deveParticionarEmLotesQuandoExceder500Tokens() throws Exception {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        BatchResponse mockBatchResponse = mock(BatchResponse.class);

        when(mockBatchResponse.getSuccessCount()).thenReturn(500);
        when(mockBatchResponse.getFailureCount()).thenReturn(0);
        when(mockMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockBatchResponse);

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> messagingStatic = mockStatic(FirebaseMessaging.class)) {

            appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 505; i++) {
                tokens.add("fcm_token_index_" + i);
            }

            fcmPushService.sendMulticastPushNow(tokens, "Aviso Geral", "Mensagem", null);

            // 505 tokens divididos em batch de 500 e batch de 5 = 2 chamadas
            verify(mockMessaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    @DisplayName("sendMulticastPushNow - Remove tokens inválidos")
    void deveRemoverTokensInvalidosEmFalhas() throws Exception {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        BatchResponse mockBatchResponse = mock(BatchResponse.class);

        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        SendResponse unregisteredResponse = mock(SendResponse.class);
        when(unregisteredResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException unregisteredEx = mock(FirebaseMessagingException.class);
        when(unregisteredEx.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(unregisteredResponse.getException()).thenReturn(unregisteredEx);

        SendResponse invalidResponse = mock(SendResponse.class);
        when(invalidResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException invalidEx = mock(FirebaseMessagingException.class);
        when(invalidEx.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(invalidResponse.getException()).thenReturn(invalidEx);

        SendResponse otherFailResponse = mock(SendResponse.class);
        when(otherFailResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException otherEx = mock(FirebaseMessagingException.class);
        when(otherEx.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(otherFailResponse.getException()).thenReturn(otherEx);

        when(mockBatchResponse.getFailureCount()).thenReturn(3);
        when(mockBatchResponse.getSuccessCount()).thenReturn(1);
        when(mockBatchResponse.getResponses()).thenReturn(List.of(
                successResponse,
                unregisteredResponse,
                invalidResponse,
                otherFailResponse
        ));
        when(mockMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockBatchResponse);

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> messagingStatic = mockStatic(FirebaseMessaging.class)) {

            appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            List<String> tokens = List.of("token_valido", "token_unregistered", "token_invalid", "token_internal_err");
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> fcmPushService.sendMulticastPushNow(tokens, "Título", "Corpo", Map.of())
            );

            verify(deviceTokenRepository).deleteByToken("token_unregistered");
            verify(deviceTokenRepository).deleteByToken("token_invalid");
            verify(deviceTokenRepository, never()).deleteByToken("token_valido");
            verify(deviceTokenRepository, never()).deleteByToken("token_internal_err");
        }
    }

    @Test
    @DisplayName("sendMulticastPushNow - Propaga falha transitória para a outbox tentar novamente")
    void propagaFalhaDoFirebaseParaRetry() throws Exception {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(new RuntimeException("Falha de rede com Firebase"));

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> messagingStatic = mockStatic(FirebaseMessaging.class)) {
            appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> fcmPushService.sendMulticastPushNow(List.of("token_1"), "Título", "Corpo", Map.of())
            );
            verifyNoInteractions(deviceTokenRepository);
        }
    }
}
