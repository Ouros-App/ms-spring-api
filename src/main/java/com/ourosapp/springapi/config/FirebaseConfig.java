package com.ourosapp.springapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Configuração de inicialização do Firebase Admin SDK.
 * Suporta credenciais via JSON direto, Base64 ou caminho de arquivo.
 * Em ambientes sem credenciais configuradas (dev/testes), opera em modo degradado sem interromper a aplicação.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-json:${FIREBASE_CREDENTIALS_JSON:}}")
    private String credentialsJson;

    @Value("${app.firebase.config-path:${FIREBASE_CONFIG_PATH:}}")
    private String configPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            InputStream serviceAccountStream = resolveCredentialsStream();
            if (serviceAccountStream == null) {
                log.warn("Credenciais do Firebase não configuradas (FIREBASE_CREDENTIALS_JSON ou FIREBASE_CONFIG_PATH ausentes). Disparo de Push Notifications FCM permanecerá desativado.");
                return null;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK inicializado com sucesso para o app: {}", app.getName());
            return app;
        } catch (Exception ex) {
            log.warn("Falha ao inicializar Firebase Admin SDK: {}. O serviço de notificações funcionará apenas in-app.", ex.getMessage());
            return null;
        }
    }

    private InputStream resolveCredentialsStream() {
        if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
            String trimmed = credentialsJson.trim();
            // Verifica se está codificado em Base64
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(trimmed);
                    return new ByteArrayInputStream(decoded);
                } catch (IllegalArgumentException ignored) {
                    // Continua para tentar usar diretamente
                }
            }
            return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
        }

        if (configPath != null && !configPath.trim().isEmpty()) {
            File file = new File(configPath.trim());
            if (file.exists() && file.isFile()) {
                try {
                    return new FileInputStream(file);
                } catch (Exception e) {
                    log.warn("Não foi possível ler o arquivo de credenciais do Firebase em '{}': {}", configPath, e.getMessage());
                }
            } else {
                InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(configPath.trim());
                if (classpathStream != null) {
                    return classpathStream;
                }
            }
        }

        return null;
    }
}
