package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.address.AddressResponseDTO;
import com.ourosapp.springapi.dto.enterprise.EnterpriseRequestDTO;
import com.ourosapp.springapi.dto.enterprise.EnterpriseResponseDTO;
import com.ourosapp.springapi.dto.enterprise.EnterpriseUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link Enterprise}.
 */
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final AddressRepository addressRepository;
    private final AddressService addressService;
    private final CompanyEmployeeRepository companyEmployeeRepository;

    /**
     * Cadastra uma nova Empresa Integradora no sistema.
     * Requer perfil ADM. Suporta criação aninhada de endereço.
     */
    @Transactional
    public EnterpriseResponseDTO createEnterprise(EnterpriseRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        if (!"ADM".equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem cadastrar empresas");
        }


        if (enterpriseRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma empresa cadastrada com este CNPJ"
            );
        }

        if (enterpriseRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma empresa cadastrada com este e-mail"
            );
        }

        Long resolvedAddressId;
        if (request.address() != null && request.idAddress() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é permitido informar 'id_address' e o objeto 'address' simultaneamente"
            );
        } else if (request.address() != null) {
            AddressResponseDTO createdAddress = addressService.createAddress(request.address());
            resolvedAddressId = createdAddress.id();
        } else if (request.idAddress() != null) {
            if (!addressRepository.existsById(request.idAddress())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + request.idAddress()
                );
            }
            resolvedAddressId = request.idAddress();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "É obrigatório informar o 'id_address' ou o objeto 'address' completo"
            );
        }

        Enterprise enterprise = Enterprise.builder()
                .name(request.name())
                .email(request.email())
                .documentNumber(request.documentNumber())
                .telephone(request.telephone())
                .idAddress(resolvedAddressId)
                .build();

        try {
            Enterprise savedEnterprise = enterpriseRepository.save(enterprise);
            return EnterpriseResponseDTO.fromEntity(savedEnterprise);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de unicidade de dados ao cadastrar empresa",
                    ex
            );
        }
    }

    /**
     * Busca as informações de uma empresa específica a partir do seu identificador único.
     */
    @Transactional(readOnly = true)
    public EnterpriseResponseDTO getEnterpriseById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada para o ID: " + id
                ));

        validateEnterpriseAccessPermission(enterprise, principal);

        return EnterpriseResponseDTO.fromEntity(enterprise);
    }

    /**
     * Retorna a lista de todas as empresas integradoras cadastradas no sistema se for ADM.
     * Se for funcionário, retorna apenas a sua empresa.
     */
    @Transactional(readOnly = true)
    public List<EnterpriseResponseDTO> getAllEnterprises(UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return enterpriseRepository.findAll()
                    .stream()
                    .map(EnterpriseResponseDTO::fromEntity)
                    .toList();
        } else if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            return enterpriseRepository.findById(employee.getIdEnterprise())
                    .map(enterprise -> List.of(EnterpriseResponseDTO.fromEntity(enterprise)))
                    .orElseGet(List::of);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil sem permissão para listar empresas"
            );
        }
    }

    /**
     * Atualiza parcialmente os dados cadastrais de uma empresa existente.
     */
    @Transactional
    public EnterpriseResponseDTO updateEnterprise(Long id, EnterpriseUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada para o ID: " + id
                ));

        validateEnterpriseMutationPermission(enterprise, principal);

        if (!request.hasUpdates()) {
            return EnterpriseResponseDTO.fromEntity(enterprise);
        }

        if (request.documentNumber() != null && !request.documentNumber().isBlank()) {
            enterpriseRepository.findByDocumentNumber(request.documentNumber())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outra empresa cadastrada com este CNPJ");
                    });
            enterprise.setDocumentNumber(request.documentNumber());
        }

        if (request.email() != null && !request.email().isBlank()) {
            enterpriseRepository.findByEmailIgnoreCase(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outra empresa cadastrada com este e-mail");
                    });
            enterprise.setEmail(request.email());
        }

        if (request.idAddress() != null) {
            if (!addressRepository.existsById(request.idAddress())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + request.idAddress()
                );
            }
            enterprise.setIdAddress(request.idAddress());
        }

        if (request.name() != null && !request.name().isBlank()) {
            enterprise.setName(request.name());
        }
        
        if (request.telephone() != null && !request.telephone().isBlank()) {
            enterprise.setTelephone(request.telephone());
        }

        try {
            Enterprise updatedEnterprise = enterpriseRepository.save(enterprise);
            return EnterpriseResponseDTO.fromEntity(updatedEnterprise);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de unicidade de dados ao atualizar empresa",
                    ex
            );
        }
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
    }

    private void validateEnterpriseAccessPermission(Enterprise enterprise, UserPrincipal principal) {
        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }
        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            if (!Objects.equals(enterprise.getId(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta empresa");
    }

    private void validateEnterpriseMutationPermission(Enterprise enterprise, UserPrincipal principal) {
        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }
        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            if (!Objects.equals(enterprise.getId(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para alterar esta empresa");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para alterar esta empresa");
    }
}
