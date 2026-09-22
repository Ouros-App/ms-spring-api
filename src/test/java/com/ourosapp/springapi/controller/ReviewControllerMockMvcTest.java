package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.ReviewService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link ReviewController}.
 */
@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

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

    // =========================================================================
    // POST /tips/{tipId}/reviews TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /tips/{tipId}/reviews - Deve cadastrar avaliação e retornar 201 Created com cabeçalho Location")
    void testCreateReviewSuccess() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO(
                "Excelente dica operacional",
                5
        );
        ReviewResponseDTO response = new ReviewResponseDTO(
                1L,
                "Excelente dica operacional",
                5,
                10L
        );

        when(reviewService.createReview(eq(10L), any(ReviewRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/tips/10/reviews")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/reviews/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.comment").value("Excelente dica operacional"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.id_tip").value(10L));
    }

    @Test
    @DisplayName("POST /tips/{tipId}/reviews - Deve retornar 400 Bad Request para payload com dados inválidos")
    void testCreateReviewInvalidPayloadBadRequest() throws Exception {
        String invalidPayload = """
                {
                    "comment": "",
                    "rating": 10
                }
                """;

        mockMvc.perform(post("/tips/10/reviews")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /tips/{tipId}/reviews - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateReviewUnauthorized() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO("Comentário", 5);

        mockMvc.perform(post("/tips/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /tips/{tipId}/reviews - Deve retornar 403 Forbidden quando usuário não tem permissão")
    void testCreateReviewForbidden() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO("Comentário", 5);
        when(reviewService.createReview(eq(10L), any(ReviewRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/tips/10/reviews")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /tips/{tipId}/reviews - Deve retornar 404 Not Found quando dica não existir")
    void testCreateReviewTipNotFound() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO("Comentário", 5);
        when(reviewService.createReview(eq(99L), any(ReviewRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Dica técnica não encontrada"));

        mockMvc.perform(post("/tips/99/reviews")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /tips/{tipId}/reviews TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /tips/{tipId}/reviews - Deve retornar 200 OK com lista de avaliações da dica")
    void testGetReviewsByTipIdSuccess() throws Exception {
        ReviewResponseDTO item = new ReviewResponseDTO(1L, "Comentário", 5, 10L);
        when(reviewService.getReviewsByTipId(eq(10L), eq(mockPrincipal)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/tips/10/reviews")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].comment").value("Comentário"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].id_tip").value(10L));
    }

    @Test
    @DisplayName("GET /tips/{tipId}/reviews - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetReviewsByTipIdUnauthorized() throws Exception {
        mockMvc.perform(get("/tips/10/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /tips/{tipId}/reviews - Deve retornar 404 Not Found quando dica não existir")
    void testGetReviewsByTipIdNotFound() throws Exception {
        when(reviewService.getReviewsByTipId(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Dica técnica não encontrada"));

        mockMvc.perform(get("/tips/99/reviews")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /reviews/{id} TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /reviews/{id} - Deve retornar 200 OK com os detalhes da avaliação")
    void testGetReviewByIdSuccess() throws Exception {
        ReviewResponseDTO response = new ReviewResponseDTO(1L, "Comentário", 5, 10L);
        when(reviewService.getReviewById(eq(1L), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(get("/reviews/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.comment").value("Comentário"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.id_tip").value(10L));
    }

    @Test
    @DisplayName("GET /reviews/{id} - Deve retornar 404 Not Found quando avaliação não existir")
    void testGetReviewByIdNotFound() throws Exception {
        when(reviewService.getReviewById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        mockMvc.perform(get("/reviews/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // PATCH /reviews/{id} TESTS
    // =========================================================================

    @Test
    @DisplayName("PATCH /reviews/{id} - Deve atualizar parcialmente avaliação e retornar 200 OK")
    void testUpdateReviewSuccess() throws Exception {
        ReviewUpdateDTO request = new ReviewUpdateDTO("Novo comentário", 4);
        ReviewResponseDTO response = new ReviewResponseDTO(1L, "Novo comentário", 4, 10L);

        when(reviewService.updateReview(eq(1L), any(ReviewUpdateDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(patch("/reviews/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.comment").value("Novo comentário"))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @DisplayName("PATCH /reviews/{id} - Deve retornar 400 Bad Request quando nota for inválida")
    void testUpdateReviewInvalidRatingBadRequest() throws Exception {
        String invalidPayload = """
                {
                    "rating": -1
                }
                """;

        mockMvc.perform(patch("/reviews/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /reviews/{id} - Deve retornar 404 Not Found quando avaliação não existir")
    void testUpdateReviewNotFound() throws Exception {
        ReviewUpdateDTO request = new ReviewUpdateDTO("Novo comentário", 4);
        when(reviewService.updateReview(eq(99L), any(ReviewUpdateDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        mockMvc.perform(patch("/reviews/99")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // DELETE /reviews/{id} TESTS
    // =========================================================================

    @Test
    @DisplayName("DELETE /reviews/{id} - Deve remover avaliação e retornar 204 No Content")
    void testDeleteReviewSuccess() throws Exception {
        doNothing().when(reviewService).deleteReview(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/reviews/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /reviews/{id} - Deve retornar 404 Not Found quando avaliação não existir")
    void testDeleteReviewNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"))
                .when(reviewService).deleteReview(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/reviews/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /reviews/{id} - Deve retornar 403 Forbidden quando usuário não tem permissão")
    void testDeleteReviewForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(reviewService).deleteReview(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/reviews/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }
}
