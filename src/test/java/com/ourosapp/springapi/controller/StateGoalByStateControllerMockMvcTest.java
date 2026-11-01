package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.stategoal.*;
import com.ourosapp.springapi.security.*;
import com.ourosapp.springapi.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StateGoalByStateController.class)
@Import(SecurityConfig.class)
class StateGoalByStateControllerMockMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean StateGoalByStateService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean KeycloakJwtAuthenticationConverter converter;
    @MockitoBean UserDetailsServiceImpl userDetails;
    private final UserPrincipal principal = new UserPrincipal(1L,"adm@example.com",null,"ADM",List.of());
    private static final String BODY = """
        {"state":" sp ","id_enterprise":2,"goal":{"title":"Realizar manutenção","date_creation":"2026-01-01","date_end":"2026-02-01"}}
        """;

    @Test void deveCriarMetaComLocationEQuantidadeRealDeParticipantes() throws Exception {
        when(service.create(any(),any())).thenReturn(new StateGoalByStateResponseDTO(10L,"SP",2L,3));
        mvc.perform(post("/state-goals/by-state").with(user(principal)).contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location","http://localhost/state-goals/10"))
                .andExpect(jsonPath("$.participants_count").value(3));
        verify(service).create(argThat(request -> request.state().equals("SP")),any());
    }
    @Test void deveListarEstadosElegiveis() throws Exception {
        when(service.listEligibleStates(eq(2L), any())).thenReturn(List.of("MG", "SP"));
        mvc.perform(get("/state-goals/states").param("id_enterprise", "2").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("MG"))
                .andExpect(jsonPath("$[1]").value("SP"));
    }

    @Test void deveConsultarPublicoDaUfEEmpresa() throws Exception {
        when(service.preview(eq("SP"),eq(2L),any())).thenReturn(new StateGoalAudienceDTO("SP",2L,0));
        mvc.perform(get("/state-goals/audience").param("state","SP").param("id_enterprise","2").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participants_count").value(0));
    }
    @Test void deveValidarUfEDadosAninhadosAntesDeGravar() throws Exception {
        for (String invalid : List.of(BODY.replace(" sp ","XX"), BODY.replace("Realizar manutenção",""),
                BODY.replace("2026-02-01","2025-01-01"), "{\"state\":\"SP\"}")) {
            mvc.perform(post("/state-goals/by-state").with(user(principal)).contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void deveExigirAutenticacao() throws Exception {
        mvc.perform(post("/state-goals/by-state").contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(get("/state-goals/audience").param("state","SP")).andExpect(status().isUnauthorized());
    }
}
