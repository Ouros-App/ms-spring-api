package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência e consulta da entidade {@link DeviceToken}.
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * Busca todos os tokens de dispositivos registrados para um usuário específico.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @return lista de tokens de dispositivo
     */
    List<DeviceToken> findAllByUserIdAndUserRole(Long userId, String userRole);

    /**
     * Busca todos os tokens de dispositivos para uma lista de usuários de um determinado perfil.
     *
     * @param userRole perfil dos usuários
     * @param userIds coleção de identificadores dos usuários
     * @return lista de tokens de dispositivo encontrados
     */
    List<DeviceToken> findAllByUserRoleAndUserIdIn(String userRole, Collection<Long> userIds);

    /**
     * Busca um token de dispositivo pelo valor único do token FCM.
     *
     * @param token string do token FCM
     * @return Optional contendo o token se encontrado
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * Remove um token de dispositivo pelo seu valor único.
     *
     * @param token string do token FCM a ser removido
     */
    void deleteByToken(String token);

    /**
     * Remove um token específico vinculado a um usuário.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @param token string do token
     */
    void deleteByUserIdAndUserRoleAndToken(Long userId, String userRole, String token);
}
