package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.client.AudienceEngineClient;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.TipAudienceEnricher;
import com.ourosapp.springapi.service.TipService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link TipController}.
 */
@WebMvcTest(TipController.class)
@Import({SecurityConfig.class, TipAudienceEnricher.class})
class TipControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TipService tipService;

    @MockitoBean
    private AudienceEngineClient audienceEngineClient;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "password123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("POST /tips - Deve cadastrar dica técnica e retornar 201 Created com Location")
    void deveCadastrarDicaComSucesso() throws Exception {
        TipRequestDTO request = new TipRequestDTO(
                "Manejo de Ambiência",
                "Manter ventilação mínima no galpão.",
                "https://ouros.com/fotos/dica1.jpg",
                1L,
                List.of(1L, 2L)
        );
        TipResponseDTO response = new TipResponseDTO(
                10L,
                "Manejo de Ambiência",
                "Manter ventilação mínima no galpão.",
                "https://ouros.com/fotos/dica1.jpg",
                1L,
                List.of("Manejo de Ambiência", "Sanitização"),
                0,
                0.0
        );

        when(tipService.createTip(any(TipRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/tips")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/tips/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Manejo de Ambiência"))
                .andExpect(jsonPath("$.tip").value("Manter ventilação mínima no galpão."))
                .andExpect(jsonPath("$.image_url").value("https://ouros.com/fotos/dica1.jpg"))
                .andExpect(jsonPath("$.foto_url").value("https://ouros.com/fotos/dica1.jpg"))
                .andExpect(jsonPath("$.id_farm").value(1L))
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.total_reviews").value(0))
                .andExpect(jsonPath("$.average_rating").value(0.0));
    }

    @Test
    @DisplayName("POST /tips - Deve cadastrar dica com imagem Cloudinary em WebP e Markdown denso")
    void deveCadastrarDicaComMarkdownEImageUrlCloudinary() throws Exception {
        String cloudinaryUrl = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp";
        String markdownTip = """
                * **Meta Diária:** Ajustar vazão do nipple para 60-80 ml/min.
                * **Inspeção:**
                  1. Checar pressão nos reguladores.
                  2. Limpar pontas de linha.
                """;

        TipRequestDTO request = new TipRequestDTO(
                "Regulagem Diária e Ajuste de Altura das Linhas de Nipple",
                markdownTip,
                cloudinaryUrl,
                null,
                1L,
                List.of(1L)
        );
        TipResponseDTO response = new TipResponseDTO(
                15L,
                "Regulagem Diária e Ajuste de Altura das Linhas de Nipple",
                markdownTip,
                cloudinaryUrl,
                cloudinaryUrl,
                1L,
                List.of("Manejo Hídrico"),
                0,
                0.0
        );

        when(tipService.createTip(any(TipRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/tips")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/tips/15")))
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.title").value("Regulagem Diária e Ajuste de Altura das Linhas de Nipple"))
                .andExpect(jsonPath("$.tip").value(markdownTip))
                .andExpect(jsonPath("$.image_url").value(cloudinaryUrl))
                .andExpect(jsonPath("$.foto_url").value(cloudinaryUrl));
    }

    @Test
    @DisplayName("POST /tips - Deve retornar 400 Bad Request se payload for inválido")
    void deveRetornarBadRequestQuandoPayloadInvalido() throws Exception {
        TipRequestDTO invalidRequest = new TipRequestDTO("", null, null);

        mockMvc.perform(post("/tips")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tips - Deve listar dicas e retornar 200 OK com image_url e foto_url")
    void deveListarDicasComSucesso() throws Exception {
        List<TipResponseDTO> responses = List.of(
                new TipResponseDTO(1L, "Título 1", "Dica 1", "https://ouros.com/1.jpg", 1L, List.of("Ambiência"), 2, 4.5),
                new TipResponseDTO(2L, "Título 2", "Dica 2", null, 1L, List.of("Nutrição"), 0, 0.0)
        );

        when(tipService.getTipsForUser(eq(null), eq(mockPrincipal))).thenReturn(responses);

        mockMvc.perform(get("/tips")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Título 1"))
                .andExpect(jsonPath("$[0].tip").value("Dica 1"))
                .andExpect(jsonPath("$[0].image_url").value("https://ouros.com/1.jpg"))
                .andExpect(jsonPath("$[0].foto_url").value("https://ouros.com/1.jpg"))
                .andExpect(jsonPath("$[0].total_reviews").value(2))
                .andExpect(jsonPath("$[0].average_rating").value(4.5))
                .andExpect(jsonPath("$[0].audience_match").doesNotExist());
    }

    @Test
    @DisplayName("GET /tips - Deve indicar as dicas pertencentes às audiências do produtor")
    void shouldReportAudienceMatchForProducerTips() throws Exception {
        UserPrincipal farmOwner = farmOwner();
        List<TipResponseDTO> responses = List.of(
                new TipResponseDTO(101L, "Dica da audiência", "Texto", null, 1L, List.of("Manejo"), 0, 0.0),
                new TipResponseDTO(102L, "Outra dica", "Texto", null, 1L, List.of("Manejo"), 0, 0.0)
        );
        when(tipService.getTipsForUser(eq(null), eq(farmOwner))).thenReturn(responses);
        when(audienceEngineClient.getTipIdsForUser(42L, "Bearer test-token")).thenReturn(Set.of(101L));
        mockFarmOwnerJwt(farmOwner);

        mockMvc.perform(get("/tips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .with(user(farmOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].audience_match").value(true))
                .andExpect(jsonPath("$[1].audience_match").value(false));
    }

    @Test
    @DisplayName("GET /tips?farm_id=1 - Deve listar dicas filtrando por fazenda")
    void deveListarDicasComFiltroDeFazenda() throws Exception {
        List<TipResponseDTO> responses = List.of(
                new TipResponseDTO(1L, "Título 1", "Dica 1", null, 1L, List.of("Ambiência"), 1, 5.0)
        );

        when(tipService.getTipsForUser(eq(1L), eq(mockPrincipal))).thenReturn(responses);

        mockMvc.perform(get("/tips")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Título 1"));
    }

    @Test
    @DisplayName("GET /tips/{id} - Deve retornar detalhes de uma dica específica com image_url e foto_url")
    void deveBuscarDicaPorIdComSucesso() throws Exception {
        TipResponseDTO response = new TipResponseDTO(1L, "Dica Especial", "Dica detalhada", "https://ouros.com/banner.jpg", 1L, List.of("Biosseguridade"), 1, 4.0);

        when(tipService.getTipById(eq(1L), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(get("/tips/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Dica Especial"))
                .andExpect(jsonPath("$.tip").value("Dica detalhada"))
                .andExpect(jsonPath("$.image_url").value("https://ouros.com/banner.jpg"))
                .andExpect(jsonPath("$.foto_url").value("https://ouros.com/banner.jpg"));
    }

    @Test
    @DisplayName("GET /tips/{id} - Inclui correspondência quando a dica pertence à audiência")
    void shouldIncludeAudienceMatchInTipDetails() throws Exception {
        UserPrincipal farmOwner = farmOwner();
        TipResponseDTO tip = new TipResponseDTO(
                101L, "Dica da audiência", "Texto", null, 1L, List.of("Manejo"), 0, 0.0
        );
        when(tipService.getTipById(eq(101L), eq(farmOwner))).thenReturn(tip);
        when(audienceEngineClient.getTipIdsForUser(42L, "Bearer test-token")).thenReturn(Set.of(101L));
        mockFarmOwnerJwt(farmOwner);

        mockMvc.perform(get("/tips/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .with(user(farmOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audience_match").value(true));
    }

    @Test
    @DisplayName("GET /tips/{id} - Omite correspondência quando o Audience Engine falha")
    void shouldOmitAudienceMatchInTipDetailsWhenAudienceEngineFails() throws Exception {
        UserPrincipal farmOwner = farmOwner();
        TipResponseDTO tip = new TipResponseDTO(
                101L, "Dica da audiência", "Texto", null, 1L, List.of("Manejo"), 0, 0.0
        );
        when(tipService.getTipById(eq(101L), eq(farmOwner))).thenReturn(tip);
        when(audienceEngineClient.getTipIdsForUser(42L, "Bearer test-token"))
                .thenThrow(new RestClientException("unavailable"));
        mockFarmOwnerJwt(farmOwner);

        mockMvc.perform(get("/tips/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .with(user(farmOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audience_match").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /tips/{id} - Deve atualizar parcialmente uma dica técnica")
    void deveAtualizarDicaComSucesso() throws Exception {
        TipUpdateDTO updateDTO = new TipUpdateDTO("Dica atualizada", List.of(2L));
        TipResponseDTO response = new TipResponseDTO(1L, "Dica Operacional", "Dica atualizada", null, 1L, List.of("Nutrição"), 0, 0.0);

        when(tipService.updateTip(eq(1L), any(TipUpdateDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(patch("/tips/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Dica Operacional"))
                .andExpect(jsonPath("$.tip").value("Dica atualizada"));
    }

    @Test
    @DisplayName("DELETE /tips/{id} - Deve remover dica técnica e retornar 204 No Content")
    void deveRemoverDicaComSucesso() throws Exception {
        doNothing().when(tipService).deleteTip(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/tips/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    private static UserPrincipal farmOwner() {
        return new UserPrincipal(
                42L,
                "produtor@ouros.com",
                "password123",
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    private void mockFarmOwnerJwt(UserPrincipal farmOwner) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", String.valueOf(farmOwner.getId()))
                .build();
        when(jwtDecoder.decode("test-token")).thenReturn(jwt);
        when(keycloakJwtAuthenticationConverter.convert(jwt)).thenReturn(
                new UsernamePasswordAuthenticationToken(farmOwner, "test-token", farmOwner.getAuthorities())
        );
    }
}
