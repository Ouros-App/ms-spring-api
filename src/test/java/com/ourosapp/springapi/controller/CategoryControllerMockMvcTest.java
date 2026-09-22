package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.category.CategoryRequestDTO;
import com.ourosapp.springapi.dto.category.CategoryResponseDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.CategoryService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link CategoryController}.
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

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
    @DisplayName("POST /categories - Deve cadastrar categoria e retornar 201 Created com Location")
    void deveCadastrarCategoriaComSucesso() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("Ambiência", 10L);
        CategoryResponseDTO response = new CategoryResponseDTO(1L, "Ambiência", 10L);

        when(categoryService.createCategory(any(CategoryRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/categories")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/categories/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.category").value("Ambiência"))
                .andExpect(jsonPath("$.id_tip").value(10L));
    }

    @Test
    @DisplayName("POST /categories - Deve retornar 400 Bad Request se corpo for inválido")
    void deveRetornarBadRequestQuandoInvalido() throws Exception {
        CategoryRequestDTO invalidRequest = new CategoryRequestDTO("", null);

        mockMvc.perform(post("/categories")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /categories - Deve listar categorias e retornar 200 OK")
    void deveListarCategoriasComSucesso() throws Exception {
        List<CategoryResponseDTO> responses = List.of(
                new CategoryResponseDTO(1L, "Ambiência", 10L),
                new CategoryResponseDTO(2L, "Sanitização", 10L)
        );

        when(categoryService.getCategories(eq(mockPrincipal))).thenReturn(responses);

        mockMvc.perform(get("/categories")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("Ambiência"))
                .andExpect(jsonPath("$[1].category").value("Sanitização"));
    }
}
