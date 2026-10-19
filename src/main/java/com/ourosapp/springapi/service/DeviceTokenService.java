package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.ErrorMessages;
import com.ourosapp.springapi.dto.notification.DeviceTokenRequestDTO;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelo gerenciamento de tokens de dispositivos (FCM) associados aos usuários da plataforma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Registra ou atualiza um token FCM para o usuário autenticado.
     *
     * @param principal usuário autenticado
     * @param dto dados do token e plataforma
     * @return entidade DeviceToken salva
     */
    @Transactional
    public DeviceToken registerOrUpdateToken(UserPrincipal principal, DeviceTokenRequestDTO dto) {
        ensureAuthenticated(principal);
        Objects.requireNonNull(dto, "Dados do dispositivo não podem ser nulos");

        String token = dto.token();
        String platform = dto.platform();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .map(existing -> {
                    existing.setUserId(principal.getId());
                    existing.setUserRole(principal.getRole());
                    existing.setPlatform(platform);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .userId(principal.getId())
                        .userRole(principal.getRole())
                        .token(token)
                        .platform(platform)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
                );

        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("Token FCM registrado com sucesso para o usuário ID {} ({})", principal.getId(), principal.getRole());
        return saved;
    }

    /**
     * Remove o vínculo de um token específico pertencente ao usuário autenticado (logout).
     *
     * @param principal usuário autenticado
     * @param token token FCM a ser desvinculado
     */
    @Transactional
    public void deleteToken(UserPrincipal principal, String token) {
        ensureAuthenticated(principal);
        if (token != null && !token.trim().isEmpty()) {
            deviceTokenRepository.deleteByUserIdAndUserRoleAndToken(principal.getId(), principal.getRole(), token.trim());
            log.info("Token FCM desvinculado para o usuário ID {} ({})", principal.getId(), principal.getRole());
        }
    }

    /**
     * Retorna todos os tokens registrados para um determinado usuário.
     *
     * @param userId identificador do usuário
     * @param userRole perfil do usuário
     * @return lista de tokens de dispositivo
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getTokensForUser(Long userId, String userRole) {
        return deviceTokenRepository.findAllByUserIdAndUserRole(userId, userRole);
    }

    /**
     * Retorna todos os tokens registrados para uma coleção de usuários com o mesmo perfil.
     *
     * @param userRole perfil dos usuários
     * @param userIds coleção de identificadores dos usuários
     * @return lista de tokens de dispositivo
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getTokensForUsers(String userRole, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return deviceTokenRepository.findAllByUserRoleAndUserIdIn(userRole, userIds);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_AUTHENTICATED);
        }
    }
}
