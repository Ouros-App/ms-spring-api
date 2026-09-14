package com.ourosapp.springapi.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class InfisicalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE = "infisicalSecrets";
    private static final String DEFAULT_PATH = "/ms-spring-api";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String clientId = environment.getProperty("INFISICAL_CLIENT_ID");
        String clientSecret = environment.getProperty("INFISICAL_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            return;
        }

        String projectId = environment.getProperty("INFISICAL_PROJECT_ID");
        String environmentSlug = environment.getProperty("INFISICAL_ENVIRONMENT", "dev");
        String secretPath = environment.getProperty("INFISICAL_SECRET_PATH", DEFAULT_PATH);
        if (projectId == null) {
            return;
        }

        try {
            var sdk = new InfisicalSdk(new SdkConfig.Builder().build());
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
