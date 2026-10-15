package com.ourosapp.springapi.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseConfigTest {

    @Test
    @DisplayName("firebaseApp - Deve retornar instancia existente quando getApps() nao estiver vazio")
    void deveRetornarInstanciaExistenteQuandoAppsNaoEstiverVazio() {
        FirebaseConfig config = new FirebaseConfig();
        FirebaseApp mockApp = mock(FirebaseApp.class);

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(mockApp));
            mockedFirebaseApp.when(FirebaseApp::getInstance).thenReturn(mockApp);

            FirebaseApp result = config.firebaseApp();

            assertSame(mockApp, result);
            mockedFirebaseApp.verify(FirebaseApp::getInstance);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve retornar null quando nenhuma credencial for configurada")
    void deveRetornarNullQuandoNenhumaCredencialConfigurada() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", "");
        ReflectionTestUtils.setField(config, "configPath", "");

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();

            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve tentar resolver stream a partir de JSON direto")
    void deveTentarResolverStreamDeJsonDireto() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", "{\"type\": \"service_account\", \"project_id\": \"test\"}");
        ReflectionTestUtils.setField(config, "configPath", null);

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            // A inicializacao falhara por chave invalida do Google, mas tratara graciosamente retornando null
            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve decodificar JSON em Base64 quando nao comecar com chave ou colchete")
    void deveDecodificarJsonEmBase64() {
        FirebaseConfig config = new FirebaseConfig();
        String json = "{\"type\": \"service_account\"}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(config, "credentialsJson", base64);

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve lidar com Base64 invalido e tentar usar diretamente")
    void deveLidarComBase64Invalido() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", "not_valid_base64_!@#$");

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve tentar ler arquivo a partir de configPath existente")
    void deveTentarLerArquivoDeConfigPathExistente() throws Exception {
        File tempFile = File.createTempFile("firebase-test", ".json");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("{\"project_id\": \"temp\"}".getBytes(StandardCharsets.UTF_8));
        }

        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", null);
        ReflectionTestUtils.setField(config, "configPath", tempFile.getAbsolutePath());

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve tentar buscar no classpath quando configPath nao for arquivo no disco")
    void deveBuscarNoClasspathQuandoNaoForArquivoEmDisco() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", null);
        ReflectionTestUtils.setField(config, "configPath", "application.properties");

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve retornar null quando configPath inexistente em disco e no classpath")
    void deveRetornarNullQuandoConfigPathNaoExistir() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", null);
        ReflectionTestUtils.setField(config, "configPath", "non-existent-path/file.json");

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());

            FirebaseApp result = config.firebaseApp();
            assertNull(result);
        }
    }

    @Test
    @DisplayName("firebaseApp - Deve inicializar FirebaseApp com sucesso quando credenciais forem validas")
    void deveInicializarFirebaseAppComSucesso() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "credentialsJson", "{\"type\": \"service_account\"}");

        com.google.auth.oauth2.GoogleCredentials mockCredentials = mock(com.google.auth.oauth2.GoogleCredentials.class);
        FirebaseApp mockApp = mock(FirebaseApp.class);
        when(mockApp.getName()).thenReturn("[DEFAULT]");

        try (MockedStatic<com.google.auth.oauth2.GoogleCredentials> mockedCredentials = mockStatic(com.google.auth.oauth2.GoogleCredentials.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            mockedCredentials.when(() -> com.google.auth.oauth2.GoogleCredentials.fromStream(any())).thenReturn(mockCredentials);
            mockedFirebaseApp.when(() -> FirebaseApp.initializeApp(any(com.google.firebase.FirebaseOptions.class))).thenReturn(mockApp);

            FirebaseApp result = config.firebaseApp();

            assertSame(mockApp, result);
            verify(mockApp).getName();
        }
    }
}
