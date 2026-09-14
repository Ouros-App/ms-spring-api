package com.ourosapp.springapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.resources.AuthClient;
import com.infisical.sdk.resources.SecretsClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class InfisicalEnvironmentPostProcessorTest {

    @Test
    void deveIgnorarQuandoCredenciaisNaoEstaoConfiguradas() {
        var environment = new MockEnvironment();

        new InfisicalEnvironmentPostProcessor(() -> {
            throw new AssertionError("SDK não deveria ser criado");
        }).postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains("infisicalSecrets")).isFalse();
    }

    @Test
    void deveIgnorarQuandoProjetoNaoEstaConfigurado() {
        var environment = new MockEnvironment()
                .withProperty("INFISICAL_CLIENT_ID", "client")
                .withProperty("INFISICAL_CLIENT_SECRET", "secret")
                .withProperty("INFISICAL_ENVIRONMENT", "dev");

        new InfisicalEnvironmentPostProcessor(() -> {
            throw new AssertionError("SDK não deveria ser criado");
        }).postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains("infisicalSecrets")).isFalse();
    }

    @Test
    void deveExigirAmbienteExplicito() {
        var environment = new MockEnvironment()
                .withProperty("INFISICAL_CLIENT_ID", "client")
                .withProperty("INFISICAL_CLIENT_SECRET", "secret")
                .withProperty("INFISICAL_PROJECT_ID", "project");

        assertThatThrownBy(() -> new InfisicalEnvironmentPostProcessor(() -> mock(InfisicalSdk.class))
                .postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INFISICAL_ENVIRONMENT deve ser informado");
    }

    @Test
    void deveCarregarSecretsNoAmbienteConfigurado() throws Exception {
        var environment = new MockEnvironment()
                .withProperty("INFISICAL_CLIENT_ID", "client")
                .withProperty("INFISICAL_CLIENT_SECRET", "secret")
                .withProperty("INFISICAL_PROJECT_ID", "project")
                .withProperty("INFISICAL_ENVIRONMENT", "prod")
                .withProperty("INFISICAL_SECRET_PATH", "/ms-spring-api");
        var sdk = mock(InfisicalSdk.class);
        var auth = mock(AuthClient.class);
        var secretsClient = mock(SecretsClient.class);
        var secret = mock(Secret.class);
        when(sdk.Auth()).thenReturn(auth);
        when(sdk.Secrets()).thenReturn(secretsClient);
        when(secretsClient.ListSecrets("project", "prod", "/ms-spring-api", false, false, false, false))
                .thenReturn(List.of(secret));
        when(secret.getSecretKey()).thenReturn("spring.datasource.url");
        when(secret.getSecretValue()).thenReturn("jdbc:test");

        new InfisicalEnvironmentPostProcessor(() -> sdk).postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:test");
        assertThat(environment.getPropertySources().get("infisicalSecrets"))
                .isInstanceOf(MapPropertySource.class);
    }

    @Test
    void deveUsarOrdemDePrioridadeMaxima() {
        assertThat(new InfisicalEnvironmentPostProcessor().getOrder())
                .isEqualTo(Integer.MIN_VALUE);
    }
}
