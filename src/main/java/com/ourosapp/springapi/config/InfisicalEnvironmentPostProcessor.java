package com.ourosapp.springapi.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class InfisicalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE = "infisicalSecrets";
    private static final String DEFAULT_PATH = "/ms-spring-api";
    private final Supplier<InfisicalSdk> sdkFactory;
    private final Path dotenvPath;

    public InfisicalEnvironmentPostProcessor() {
        this(() -> new InfisicalSdk(new SdkConfig.Builder().build()), Path.of(".env"));
    }

    InfisicalEnvironmentPostProcessor(Supplier<InfisicalSdk> sdkFactory) {
        this(sdkFactory, null);
    }

    InfisicalEnvironmentPostProcessor(Supplier<InfisicalSdk> sdkFactory, Path dotenvPath) {
        this.sdkFactory = sdkFactory;
        this.dotenvPath = dotenvPath;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadDotEnv(environment);
        String clientId = environment.getProperty("INFISICAL_CLIENT_ID");
        String clientSecret = environment.getProperty("INFISICAL_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            return;
        }

        String projectId = environment.getProperty("INFISICAL_PROJECT_ID");
        String environmentSlug = environment.getProperty("INFISICAL_ENVIRONMENT");
        if (environmentSlug == null || environmentSlug.isBlank()) {
            throw new IllegalStateException("INFISICAL_ENVIRONMENT deve ser informado");
        }
        String secretPath = environment.getProperty("INFISICAL_SECRET_PATH", DEFAULT_PATH);
        if (projectId == null) {
            return;
        }

        try {
            var sdk = sdkFactory.get();
            sdk.Auth().UniversalAuthLogin(clientId, clientSecret);
            List<Secret> secrets = sdk.Secrets().ListSecrets(projectId, environmentSlug, secretPath, false, false, false, false);
            Map<String, Object> values = new HashMap<>();
            for (Secret secret : secrets) {
                values.put(secret.getSecretKey(), secret.getSecretValue());
            }
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE, values));
        } catch (InfisicalException exception) {
            throw new IllegalStateException("Não foi possível carregar os secrets do Infisical", exception);
        }
    }

    private void loadDotEnv(ConfigurableEnvironment environment) {
        if (dotenvPath == null || !Files.isRegularFile(dotenvPath)) {
            return;
        }

        Map<String, Object> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(dotenvPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                int separator = trimmed.indexOf('=');
                if (separator <= 0 || trimmed.startsWith("#")) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                if (!key.startsWith("INFISICAL_") || environment.getProperty(key) != null) {
                    continue;
                }
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o arquivo .env", exception);
        }

        if (!values.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", values));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
