package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.notification.NotificationResponseDTO;
import com.ourosapp.springapi.dto.notification.UnreadCountResponseDTO;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.NotificationRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link NotificationService}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UserPrincipal principal;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(
                10L,
                "user@ouros.com",
                "secret",
                RoleConstants.FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        sampleNotification = Notification.builder()
                .id(1L)
                .userId(10L)
                .userRole(RoleConstants.FARM_OWNER)
                .title("Ranking Atualizado!")
                .message("Sua fazenda ficou em 1º lugar.")
                .type("RANKING_UPDATED")
                .dataPayload("{\"farm_id\":10}")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar todas as notificações do usuário quando unreadOnly for nulo ou falso")
    void deveRetornarTodasAsNotificacoesDoUsuario() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(sampleNotification));

        when(notificationRepository.findAllByUserIdAndUserRoleOrderByCreatedAtDesc(10L, RoleConstants.FARM_OWNER, pageable))
                .thenReturn(page);

        Page<NotificationResponseDTO> result = notificationService.getUserNotifications(principal, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Ranking Atualizado!", result.getContent().get(0).title());
        verify(notificationRepository).findAllByUserIdAndUserRoleOrderByCreatedAtDesc(10L, RoleConstants.FARM_OWNER, pageable);
    }

    @Test
    @DisplayName("Deve filtrar apenas notificações não lidas quando unreadOnly for verdadeiro")
    void deveFiltrarApenasNotificacoesNaoLidas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(sampleNotification));

        when(notificationRepository.findAllByUserIdAndUserRoleAndIsReadOrderByCreatedAtDesc(10L, RoleConstants.FARM_OWNER, false, pageable))
                .thenReturn(page);

        Page<NotificationResponseDTO> result = notificationService.getUserNotifications(principal, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findAllByUserIdAndUserRoleAndIsReadOrderByCreatedAtDesc(10L, RoleConstants.FARM_OWNER, false, pageable);
    }

    @Test
    @DisplayName("Deve lançar 401 ao buscar notificações sem autenticação")
    void deveLancar401AoBuscarNotificacoesSemAutenticacao() {
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.getUserNotifications(null, false, pageable)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("Deve retornar a contagem correta de notificações não lidas")
    void deveRetornarContagemDeNaoLidas() {
        when(notificationRepository.countByUserIdAndUserRoleAndIsReadFalse(10L, RoleConstants.FARM_OWNER))
                .thenReturn(5L);

        UnreadCountResponseDTO result = notificationService.getUnreadCount(principal);

        assertNotNull(result);
        assertEquals(5L, result.unreadCount());
    }

    @Test
    @DisplayName("Deve marcar notificação como lida com sucesso quando pertencer ao usuário")
    void deveMarcarNotificacaoComoLidaComSucesso() {
        when(notificationRepository.findByIdAndUserIdAndUserRole(1L, 10L, RoleConstants.FARM_OWNER))
                .thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDTO result = notificationService.markAsRead(1L, principal);

        assertNotNull(result);
        assertTrue(result.isRead());
        assertNotNull(result.readAt());
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    @DisplayName("Deve retornar notificação sem alterar quando já estiver marcada como lida")
    void deveRetornarNotificacaoJaLidaSemSalvarNovamente() {
        sampleNotification.setRead(true);
        sampleNotification.setReadAt(LocalDateTime.now().minusHours(1));

        when(notificationRepository.findByIdAndUserIdAndUserRole(1L, 10L, RoleConstants.FARM_OWNER))
                .thenReturn(Optional.of(sampleNotification));

        NotificationResponseDTO result = notificationService.markAsRead(1L, principal);

        assertNotNull(result);
        assertTrue(result.isRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 404 quando notificação não for encontrada para o usuário")
    void deveLancar404QuandoNotificacaoNaoEncontrada() {
        when(notificationRepository.findByIdAndUserIdAndUserRole(99L, 10L, RoleConstants.FARM_OWNER))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(99L, principal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve marcar todas as notificações do usuário como lidas")
    void deveMarcarTodasComoLidas() {
        when(notificationRepository.markAllAsReadByUserIdAndUserRole(eq(10L), eq(RoleConstants.FARM_OWNER), any(LocalDateTime.class)))
                .thenReturn(3);

        notificationService.markAllAsRead(principal);

        verify(notificationRepository).markAllAsReadByUserIdAndUserRole(eq(10L), eq(RoleConstants.FARM_OWNER), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve criar e salvar nova notificação in-app com sucesso")
    void deveCriarNotificacaoComSucesso() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification created = notificationService.createNotification(
                10L,
                RoleConstants.FARM_OWNER,
                "Título Teste",
                "Mensagem Teste",
                "RANKING_UPDATED",
                "{\"key\":\"val\"}"
        );

        assertNotNull(created);
        assertEquals(10L, created.getUserId());
        assertEquals("Título Teste", created.getTitle());
        assertEquals("Mensagem Teste", created.getMessage());
        assertFalse(created.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }
}
