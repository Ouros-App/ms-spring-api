package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.payment.PaymentRequestDTO;
import com.ourosapp.springapi.dto.payment.PaymentResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnterprisePlan;
import com.ourosapp.springapi.entity.Payment;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterprisePlanRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.PaymentRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelas regras de negócio, validações de consistência e persistência de pagamentos.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnterprisePlanRepository enterprisePlanRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long enterpriseId = resolveAndValidateEnterpriseId(request.idEnterprise(), principal, "registrar pagamentos para");

        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa integradora não encontrada para o ID: " + enterpriseId);
        }

        EnterprisePlan enterprisePlan = enterprisePlanRepository.findById(request.idEnterprisePlan())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vínculo de plano da empresa não encontrado para o ID: " + request.idEnterprisePlan()
                ));

        // Validação crucial da FK Composta: id_enterprise_plan deve pertencer à mesma id_enterprise
        if (!enterprisePlan.getIdEnterprise().equals(enterpriseId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inconsistência de dados: o plano da empresa (ID: " + enterprisePlan.getId() +
                    ") pertence à empresa ID " + enterprisePlan.getIdEnterprise() +
                    ", mas o pagamento foi solicitado para a empresa ID " + enterpriseId
            );
        }

        Payment payment = Payment.builder()
                .type(request.type())
                .value(request.value())
                .dateCreation(LocalDateTime.now())
                .idEnterprise(enterpriseId)
                .idEnterprisePlan(enterprisePlan.getId())
                .build();

        try {
            Payment saved = paymentRepository.save(payment);
            return PaymentResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade ao registrar pagamento", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPayments(Long filterEnterpriseId, Long filterEnterprisePlanId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (RoleConstants.COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (filterEnterpriseId != null && !filterEnterpriseId.equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: colaboradores só podem consultar pagamentos da sua própria empresa");
            }
            filterEnterpriseId = userEnterpriseId;
        } else if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para este perfil de usuário");
        }

        List<Payment> list;
        if (filterEnterpriseId != null && filterEnterprisePlanId != null) {
            list = paymentRepository.findByIdEnterpriseAndIdEnterprisePlan(filterEnterpriseId, filterEnterprisePlanId);
        } else if (filterEnterpriseId != null) {
            list = paymentRepository.findByIdEnterprise(filterEnterpriseId);
        } else if (filterEnterprisePlanId != null) {
            list = paymentRepository.findByIdEnterprisePlan(filterEnterprisePlanId);
        } else {
            list = paymentRepository.findAll();
        }

        return list.stream().map(PaymentResponseDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        Payment payment = findPaymentByIdOrThrow(id);

        if (RoleConstants.COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (!payment.getIdEnterprise().equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: este pagamento não pertence à sua empresa integradora");
            }
        } else if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para este perfil de usuário");
        }

        return PaymentResponseDTO.fromEntity(payment);
    }

    @Transactional
    public void deletePayment(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: apenas administradores podem estornar pagamentos");
        }

        Payment payment = findPaymentByIdOrThrow(id);
        paymentRepository.delete(payment);
    }

    private Payment findPaymentByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado para o ID: null");
        }
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado para o ID: " + id));
    }

    private Long resolveAndValidateEnterpriseId(Long requestedId, UserPrincipal principal, String actionDescription) {
        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role)) {
            if (requestedId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID da empresa integradora é obrigatório para administradores");
            }
            return requestedId;
        }
        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (requestedId != null && !requestedId.equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: colaboradores não podem " + actionDescription + " outra empresa");
            }
            return userEnterpriseId;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: perfil de usuário sem permissão para gerenciar pagamentos");
    }

    private Long getUserEnterpriseIdOrThrow(UserPrincipal principal) {
        return companyEmployeeRepository.findById(principal.getId())
                .map(CompanyEmployee::getIdEnterprise)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Vínculo de empresa do colaborador não encontrado"));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }
}
