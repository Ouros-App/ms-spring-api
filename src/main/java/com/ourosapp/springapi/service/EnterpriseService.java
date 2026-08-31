package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.EnterpriseRequestDTO;
import com.ourosapp.springapi.dto.EnterpriseResponseDTO;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.util.CnpjValidator;
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

    /**
     * Cadastra uma nova Empresa Integradora no sistema.
     *
     * @param request dados da empresa a ser cadastrada
     * @return DTO com os dados da empresa cadastrada incluindo o ID gerado
     * @throws ResponseStatusException HTTP 400 (Bad Request) se o CNPJ for matematicamente inválido
     * @throws ResponseStatusException HTTP 404 (Not Found) se o endereço informado não existir
     * @throws ResponseStatusException HTTP 409 (Conflict) se o CNPJ ou e-mail já estiverem cadastrados
     */
    @Transactional
    public EnterpriseResponseDTO createEnterprise(EnterpriseRequestDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        if (!CnpjValidator.isValid(request.documentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CNPJ informado é inválido"
            );
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

        if (!addressRepository.existsById(request.idAddress())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Endereço não encontrado para o ID: " + request.idAddress()
            );
        }

        Enterprise enterprise = Enterprise.builder()
                .name(request.name())
                .email(request.email())
                .documentNumber(request.documentNumber())
                .telephone(request.telephone())
                .idAddress(request.idAddress())
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
     *
     * @param id identificador único da empresa
     * @return DTO com as informações da empresa encontrada
     * @throws ResponseStatusException HTTP 404 (Not Found) se a empresa não existir
     */
    @Transactional(readOnly = true)
    public EnterpriseResponseDTO getEnterpriseById(Long id) {
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada para o ID: " + id
                ));
        return EnterpriseResponseDTO.fromEntity(enterprise);
    }

    /**
     * Retorna a lista de todas as empresas integradoras cadastradas no sistema.
     *
     * @return lista contendo os DTOs de todas as empresas encontradas
     */
    @Transactional(readOnly = true)
    public List<EnterpriseResponseDTO> getAllEnterprises() {
        return enterpriseRepository.findAll()
                .stream()
                .map(EnterpriseResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Substitui integralmente os dados cadastrais de uma empresa existente por novos valores.
     *
     * @param id      identificador único da empresa a ser atualizada
     * @param request novos dados da empresa
     * @return DTO com os dados atualizados da empresa
     * @throws ResponseStatusException HTTP 400 (Bad Request) se o CNPJ for matematicamente inválido
     * @throws ResponseStatusException HTTP 404 (Not Found) se a empresa ou o endereço não existirem
     * @throws ResponseStatusException HTTP 409 (Conflict) se o CNPJ ou e-mail já pertencerem a outra empresa
     */
    @Transactional
    public EnterpriseResponseDTO updateEnterprise(Long id, EnterpriseRequestDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada para o ID: " + id
                ));

        if (!CnpjValidator.isValid(request.documentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CNPJ informado é inválido"
            );
        }

        enterpriseRepository.findByDocumentNumber(request.documentNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe outra empresa cadastrada com este CNPJ"
                    );
                });

        enterpriseRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe outra empresa cadastrada com este e-mail"
                    );
                });

        if (!addressRepository.existsById(request.idAddress())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Endereço não encontrado para o ID: " + request.idAddress()
            );
        }

        enterprise.setName(request.name());
        enterprise.setDocumentNumber(request.documentNumber());
        enterprise.setEmail(request.email());
        enterprise.setTelephone(request.telephone());
        enterprise.setIdAddress(request.idAddress());

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
}

