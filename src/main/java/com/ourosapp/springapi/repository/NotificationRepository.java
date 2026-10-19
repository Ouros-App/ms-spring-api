package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência e consulta da entidade {@link Notification}.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Busca notificações paginadas de um usuário ordenadas pela data de criação decrescente.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @param pageable parâmetros de paginação
     * @return página de notificações
     */
    Page<Notification> findAllByUserIdAndUserRoleOrderByCreatedAtDesc(Long userId, String userRole, Pageable pageable);

    /**
     * Busca notificações paginadas de um usuário filtrando por status de leitura.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @param isRead status de leitura
     * @param pageable parâmetros de paginação
     * @return página de notificações
     */
    Page<Notification> findAllByUserIdAndUserRoleAndIsReadOrderByCreatedAtDesc(Long userId, String userRole, boolean isRead, Pageable pageable);

    /**
     * Conta a quantidade de notificações não lidas de um usuário.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @return total de notificações não lidas
     */
    long countByUserIdAndUserRoleAndIsReadFalse(Long userId, String userRole);

    /**
     * Busca uma notificação específica pertencente a um usuário.
     *
     * @param id identificador da notificação
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @return Optional contendo a notificação se pertencer ao usuário
     */
    Optional<Notification> findByIdAndUserIdAndUserRole(Long id, Long userId, String userRole);

    /**
     * Marca todas as notificações não lidas do usuário como lidas.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @param readAt data/hora de leitura
     * @return quantidade de notificações atualizadas
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.userRole = :userRole AND n.isRead = false")
    int markAllAsReadByUserIdAndUserRole(@Param("userId") Long userId, @Param("userRole") String userRole, @Param("readAt") LocalDateTime readAt);
}
