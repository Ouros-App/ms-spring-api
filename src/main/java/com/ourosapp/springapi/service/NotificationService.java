package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.ErrorMessages;
import com.ourosapp.springapi.dto.notification.NotificationResponseDTO;
import com.ourosapp.springapi.dto.notification.UnreadCountResponseDTO;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.NotificationRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela gestão de notificações In-App para os usuários autenticados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Consulta as notificações paginadas do usuário autenticado, com filtro opcional por não lidas.
     *
     * @param principal  usuário autenticado
     * @param unreadOnly se verdadeiro, retorna apenas notificações não lidas
     * @param pageable   parâmetros de paginação e ordenação
     * @return página contendo os DTOs das notificações
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getUserNotifications(UserPrincipal principal, Boolean unreadOnly, Pageable pageable) {
        ensureAuthenticated(principal);

        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findAllByUserIdAndUserRoleAndIsReadOrderByCreatedAtDesc(
                    principal.getId(), principal.getRole(), false, pageable
            );
        } else {
            page = notificationRepository.findAllByUserIdAndUserRoleOrderByCreatedAtDesc(
                    principal.getId(), principal.getRole(), pageable
            );
        }

        return page.map(NotificationResponseDTO::fromEntity);
    }

    /**
     * Retorna a contagem de notificações pendentes de leitura para o usuário autenticado.
     *
     * @param principal usuário autenticado
     * @return DTO com o número de notificações não lidas
     */
    @Transactional(readOnly = true)
    public UnreadCountResponseDTO getUnreadCount(UserPrincipal principal) {
        ensureAuthenticated(principal);
        long count = notificationRepository.countByUserIdAndUserRoleAndIsReadFalse(principal.getId(), principal.getRole());
        return new UnreadCountResponseDTO(count);
    }

    /**
     * Marca uma notificação específica como lida, validando se pertence ao usuário autenticado.
     *
     * @param id        identificador da notificação
     * @param principal usuário autenticado
     * @return DTO atualizado da notificação
     */
    @Transactional
    public NotificationResponseDTO markAsRead(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Objects.requireNonNull(id, "O identificador da notificação não pode ser nulo");

        Notification notification = notificationRepository.findByIdAndUserIdAndUserRole(id, principal.getId(), principal.getRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada para o usuário informado"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            log.info("Notificação ID {} marcada como lida pelo usuário ID {}", id, principal.getId());
        }

        return NotificationResponseDTO.fromEntity(notification);
    }

    /**
     * Marca todas as notificações não lidas do usuário autenticado como lidas.
     *
     * @param principal usuário autenticado
     */
    @Transactional
    public void markAllAsRead(UserPrincipal principal) {
        ensureAuthenticated(principal);
        int updated = notificationRepository.markAllAsReadByUserIdAndUserRole(
                principal.getId(), principal.getRole(), LocalDateTime.now()
        );
        log.info("Total de {} notificações marcadas como lidas para o usuário ID {}", updated, principal.getId());
    }

    /**
     * Cria e persiste uma nova notificação in-app no banco de dados.
     *
     * @param userId      identificador do usuário destinatário
     * @param userRole    perfil do usuário destinatário
     * @param title       título da notificação
     * @param message     conteúdo da mensagem
     * @param type        tipo da notificação (ex: RANKING_UPDATED)
     * @param dataPayload JSON opcional com metadados
     * @return entidade Notification salva
     */
    @Transactional
    public Notification createNotification(Long userId, String userRole, String title, String message, String type, String dataPayload) {
        Notification notification = Notification.builder()
                .userId(userId)
                .userRole(userRole)
                .title(title)
                .message(message)
                .type(type != null ? type : "RANKING_UPDATED")
                .dataPayload(dataPayload)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Cria e persiste múltiplas notificações in-app em lote.
     *
     * @param notifications lista de notificações a serem salvas
     * @return lista de notificações persistidas
     */
    @Transactional
    public List<Notification> createNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }
        return notificationRepository.saveAll(notifications);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_AUTHENTICATED);
        }
    }
}
