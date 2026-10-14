package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.notification.DeviceTokenRequestDTO;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link DeviceTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private UserPrincipal ownerPrincipal;

    @BeforeEach
    void setUp() {
        ownerPrincipal = new UserPrincipal(
                10L,
                "owner@fazenda.com",
                "senha123",
                RoleConstants.FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    @Test
    @DisplayName("Deve registrar novo token de dispositivo quando token ainda não existir")
    void deveRegistrarNovoTokenComSucesso() {
        DeviceTokenRequestDTO dto = new DeviceTokenRequestDTO("fcm_token_123", "ANDROID");
        when(deviceTokenRepository.findByToken("fcm_token_123")).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken result = deviceTokenService.registerOrUpdateToken(ownerPrincipal, dto);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(RoleConstants.FARM_OWNER, result.getUserRole());
        assertEquals("fcm_token_123", result.getToken());
        assertEquals("ANDROID", result.getPlatform());
        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    @DisplayName("Deve atualizar dados de token existente quando token já estiver cadastrado")
    void deveAtualizarTokenExistenteComSucesso() {
        DeviceToken existing = DeviceToken.builder()
                .id(1L)
                .userId(5L)
                .userRole(RoleConstants.COMPANY_EMPLOYEE)
                .token("fcm_token_123")
                .platform("IOS")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        DeviceTokenRequestDTO dto = new DeviceTokenRequestDTO("fcm_token_123", "ANDROID");
        when(deviceTokenRepository.findByToken("fcm_token_123")).thenReturn(Optional.of(existing));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken result = deviceTokenService.registerOrUpdateToken(ownerPrincipal, dto);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(RoleConstants.FARM_OWNER, result.getUserRole());
        assertEquals("ANDROID", result.getPlatform());
        verify(deviceTokenRepository).save(existing);
    }

    @Test
    @DisplayName("Deve lançar 401 quando usuário não estiver autenticado ao registrar token")
    void deveLancarExcecaoQuandoUsuarioNaoAutenticado() {
        DeviceTokenRequestDTO dto = new DeviceTokenRequestDTO("fcm_token_123", "ANDROID");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                deviceTokenService.registerOrUpdateToken(null, dto)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(deviceTokenRepository);
    }

    @Test
    @DisplayName("Deve remover token de dispositivo com sucesso no logout")
    void deveRemoverTokenComSucesso() {
        deviceTokenService.deleteToken(ownerPrincipal, "fcm_token_123");

        verify(deviceTokenRepository).deleteByUserIdAndUserRoleAndToken(10L, RoleConstants.FARM_OWNER, "fcm_token_123");
    }

    @Test
    @DisplayName("Deve buscar tokens por usuário com sucesso")
    void deveBuscarTokensPorUsuario() {
        DeviceToken token = DeviceToken.builder()
                .id(1L)
                .userId(10L)
                .userRole(RoleConstants.FARM_OWNER)
                .token("fcm_token_123")
                .platform("ANDROID")
                .build();

        when(deviceTokenRepository.findAllByUserIdAndUserRole(10L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of(token));

        List<DeviceToken> tokens = deviceTokenService.getTokensForUser(10L, RoleConstants.FARM_OWNER);

        assertEquals(1, tokens.size());
        assertEquals("fcm_token_123", tokens.get(0).getToken());
    }

    @Test
    @DisplayName("Deve buscar tokens para lista de usuários com sucesso")
    void deveBuscarTokensParaListaDeUsuarios() {
        DeviceToken token1 = DeviceToken.builder().id(1L).userId(10L).userRole(RoleConstants.FARM_OWNER).token("t1").build();
        DeviceToken token2 = DeviceToken.builder().id(2L).userId(20L).userRole(RoleConstants.FARM_OWNER).token("t2").build();

        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(RoleConstants.FARM_OWNER, List.of(10L, 20L)))
                .thenReturn(List.of(token1, token2));

        List<DeviceToken> tokens = deviceTokenService.getTokensForUsers(RoleConstants.FARM_OWNER, List.of(10L, 20L));

        assertEquals(2, tokens.size());
    }
}
