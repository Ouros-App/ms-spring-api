package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.service.AddressService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AddressControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST /addresses - Deve criar endereço e retornar 201 Created com cabeçalho Location")
    void testCreateAddressSuccess() throws Exception {
        AddressUpdateDTO request = new AddressUpdateDTO("01310-100", "SP", "São Paulo", "1000", "BR");
        AddressResponseDTO response = new AddressResponseDTO(1L, "01310-100", "SP", "São Paulo", "1000", "BR");

        when(addressService.createAddress(any(AddressUpdateDTO.class))).thenReturn(response);

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/addresses/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.zip_code").value("01310-100"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.number").value("1000"))
                .andExpect(jsonPath("$.country").value("BR"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /addresses - Deve normalizar espaços e converter UF e país em minúsculas para maiúsculas")
    void testCreateAddressNormalizedValues() throws Exception {
        AddressResponseDTO response = new AddressResponseDTO(1L, "01310-100", "SP", "São Paulo", "1000", "BR");
        when(addressService.createAddress(eq(new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"))))
                .thenReturn(response);

        String jsonPayload = """
                {
                    "zip_code": "  01310-100  ",
                    "state": " sp ",
                    "city": "  São Paulo  ",
                    "number": " 1000 ",
                    "country": " br "
                }
                """;

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/addresses/1")))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.country").value("BR"));
    }

    @Test
    @DisplayName("POST /addresses - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateAddressUnauthorized() throws Exception {
        AddressUpdateDTO request = new AddressUpdateDTO("01310-100", "SP", "São Paulo", "1000", "BR");

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /addresses - Deve retornar 400 Bad Request quando campos obrigatórios estiverem em branco")
    void testCreateAddressInvalidPayload() throws Exception {
        AddressRequestDTO invalidRequest = new AddressRequestDTO("", "", "", "", "");

        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /addresses - Deve retornar 400 Bad Request quando formato de state ou country for inválido")
    void testCreateAddressInvalidStateAndCountryFormat() throws Exception {
        AddressRequestDTO invalidStateDigits = new AddressRequestDTO("01310-100", "12", "São Paulo", "1000", "BR");
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStateDigits)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidStateLength = new AddressRequestDTO("01310-100", "SPA", "São Paulo", "1000", "BR");
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStateLength)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidCountryDigits = new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "12");
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCountryDigits)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidCountryLength = new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BRA");
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCountryLength)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /addresses/{id} - Deve retornar 200 OK e dados do endereço quando encontrado")
    void testGetAddressByIdSuccess() throws Exception {
        AddressResponseDTO response = new AddressResponseDTO(1L, "01310-100", "SP", "São Paulo", "1000", "BR");

        when(addressService.getAddressById(1L)).thenReturn(response);

        mockMvc.perform(get("/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.zip_code").value("01310-100"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.number").value("1000"))
                .andExpect(jsonPath("$.country").value("BR"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /addresses/{id} - Deve retornar 404 Not Found quando endereço não existir")
    void testGetAddressByIdNotFound() throws Exception {
        when(addressService.getAddressById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado"));

        mockMvc.perform(get("/addresses/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /addresses/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetAddressByIdUnauthorized() throws Exception {
        mockMvc.perform(get("/addresses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /addresses/{id} - Deve atualizar endereço e retornar 200 OK quando válido")
    void testUpdateAddressSuccess() throws Exception {
        AddressUpdateDTO request = new AddressUpdateDTO("13010-001", "SP", "Campinas", "555", "BR");
        AddressResponseDTO response = new AddressResponseDTO(1L, "13010-001", "SP", "Campinas", "555", "BR");

        when(addressService.updateAddress(eq(1L), any(AddressUpdateDTO.class))).thenReturn(response);

        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.zip_code").value("13010-001"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.city").value("Campinas"))
                .andExpect(jsonPath("$.number").value("555"))
                .andExpect(jsonPath("$.country").value("BR"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /addresses/{id} - Deve retornar 404 Not Found ao tentar atualizar endereço inexistente")
    void testUpdateAddressNotFound() throws Exception {
        AddressUpdateDTO request = new AddressUpdateDTO("13010-001", "SP", "Campinas", "555", "BR");

        when(addressService.updateAddress(eq(99L), any(AddressUpdateDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado"));

        mockMvc.perform(put("/addresses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /addresses/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateAddressInvalidPayload() throws Exception {
        AddressRequestDTO invalidRequest = new AddressRequestDTO("", "", "", "", "");

        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /addresses/{id} - Deve retornar 400 Bad Request quando formato de state ou country for inválido")
    void testUpdateAddressInvalidStateAndCountryFormat() throws Exception {
        AddressRequestDTO invalidStateDigits = new AddressRequestDTO("13010-001", "12", "Campinas", "555", "BR");
        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStateDigits)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidStateLength = new AddressRequestDTO("13010-001", "SPA", "Campinas", "555", "BR");
        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStateLength)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidCountryDigits = new AddressRequestDTO("13010-001", "SP", "Campinas", "555", "12");
        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCountryDigits)))
                .andExpect(status().isBadRequest());

        AddressRequestDTO invalidCountryLength = new AddressRequestDTO("13010-001", "SP", "Campinas", "555", "BRA");
        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCountryLength)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /addresses/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testUpdateAddressUnauthorized() throws Exception {
        AddressUpdateDTO request = new AddressUpdateDTO("13010-001", "SP", "Campinas", "555", "BR");

        mockMvc.perform(put("/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
