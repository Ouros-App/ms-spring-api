package com.ourosapp.springapi.event;

import java.time.LocalDate;

/**
 * Evento disparado quando um novo lote de aves é cadastrado no sistema para uma fazenda.
 * Utilizado para desacoplar a emissão de notificações In-App e Push Notifications (FCM).
 *
 * @param lotId            identificador único do lote criado
 * @param farmId           identificador da fazenda vinculada ao lote
 * @param farmName         nome da fazenda
 * @param enterpriseId     identificador da empresa integradora
 * @param receivedChickens quantidade de aves recebidas / alojadas
 * @param deliveryDate     data prevista de entrega / abate
 * @param cost             custo inicial associado ao lote
 * @param creatorRole      perfil do usuário que criou o lote (ex: COMPANY_EMPLOYEE, ADM)
 * @param creatorId        identificador do usuário criador
 */
public record LotCreatedEvent(
        Long lotId,
        Long farmId,
        String farmName,
        Long enterpriseId,
        Integer receivedChickens,
        LocalDate deliveryDate,
        Double cost,
        String creatorRole,
        Long creatorId
) {
}
