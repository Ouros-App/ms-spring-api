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
    @DisplayName("sendMulticastPush - Deve retornar imediatamente quando a lista de tokens for nula ou vazia")
    void deveRetornarQuandoListaTokensNulaOuVazia() {
        fcmPushService.sendMulticastPush(null, "Título", "Corpo", Map.of());
        fcmPushService.sendMulticastPush(List.of(), "Título", "Corpo", Map.of());

        verifyNoInteractions(deviceTokenRepository);
    }

    @Test
    @DisplayName("sendMulticastPush - Deve ignorar push quando Firebase não estiver inicializado")
    void deveIgnorarPushQuandoFirebaseNaoInicializado() {
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            fcmPushService.sendMulticastPush(List.of("token_123"), "Título", "Corpo", Map.of("key", "val"));

            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPush - Deve ignorar envio se todos os tokens forem nulos ou em branco")
    void deveIgnorarQuandoTokensForemBrancosOuNulos() {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));

            List<String> blankTokens = new ArrayList<>();
            blankTokens.add(null);
            blankTokens.add("   ");
            blankTokens.add("");

            fcmPushService.sendMulticastPush(blankTokens, "Título", "Corpo", null);

            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPush - Deve enviar push com sucesso para tokens distintos")
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
            fcmPushService.sendMulticastPush(tokens, "Título Teste", "Corpo Teste", Map.of("id", "123"));

            verify(mockMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
            verifyNoInteractions(deviceTokenRepository);
        }
    }

    @Test
    @DisplayName("sendMulticastPush - Deve particionar em lotes quando exceder 500 tokens")
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

            fcmPushService.sendMulticastPush(tokens, "Aviso Geral", "Mensagem", null);

            // 505 tokens divididos em batch de 500 e batch de 5 = 2 chamadas
            verify(mockMessaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    @DisplayName("sendMulticastPush - Deve remover tokens inválidos quando houver falhas UNREGISTERED e INVALID_ARGUMENT")
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
            fcmPushService.sendMulticastPush(tokens, "Título", "Corpo", Map.of());

            verify(deviceTokenRepository).deleteByToken("token_unregistered");
            verify(deviceTokenRepository).deleteByToken("token_invalid");
            verify(deviceTokenRepository, never()).deleteByToken("token_valido");
            verify(deviceTokenRepository, never()).deleteByToken("token_internal_err");
        }
    }

    @Test
    @DisplayName("sendMulticastPush - Não deve quebrar quando ocorrer exceção inesperada no envio")
    void naoDeveQuebrarComExcecaoInesperada() throws Exception {
        FirebaseApp mockApp = mock(FirebaseApp.class);
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);

        when(mockMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(new RuntimeException("Falha de rede com Firebase"));

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> messagingStatic = mockStatic(FirebaseMessaging.class)) {

            appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            messagingStatic.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmPushService.sendMulticastPush(List.of("token_1"), "Título", "Corpo", Map.of());

            verifyNoInteractions(deviceTokenRepository);
        }
    }
}
