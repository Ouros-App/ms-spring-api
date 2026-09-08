package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.ACCESS_DENIED_ADDRESS;
import static com.ourosapp.springapi.constants.ErrorMessages.EMPLOYEE_NOT_FOUND;
import static com.ourosapp.springapi.constants.ErrorMessages.FARM_OWNER_NOT_FOUND;
import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.address.AddressRequestDTO;
import com.ourosapp.springapi.dto.address.AddressResponseDTO;
import com.ourosapp.springapi.dto.address.AddressUpdateDTO;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link Address}.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra um novo endereço no sistema.
     *
     * @param request dados do endereço a ser cadastrado
     * @return DTO com as informações do endereço salvo
     */
    @Transactional
    public AddressResponseDTO createAddress(AddressRequestDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        Address address = Address.builder()
                .zipCode(request.zipCode())
                .state(request.state())
                .city(request.city())
                .number(request.number())
                .country(request.country())
                .build();
        Address savedAddress = addressRepository.save(address);
        return AddressResponseDTO.fromEntity(savedAddress);
    }

    /**
     * Busca um endereço pelo seu identificador único, validando se o usuário autenticado possui permissão de acesso.
     *
     * @param id identificador do endereço
     * @param principal dados do usuário autenticado
     * @return DTO com as informações do endereço encontrado
     * @throws ResponseStatusException se não autenticado (401), não encontrado (404) ou acesso negado (403)
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + id
                ));
        validateAddressAccessPermission(address, principal);
        return AddressResponseDTO.fromEntity(address);
    }

    /**
     * Atualiza parcialmente os dados de um endereço existente, validando se o usuário autenticado possui permissão de acesso.
     *
     * @param id        identificador do endereço
     * @param request   dados para atualização (apenas os campos preenchidos serão atualizados)
     * @param principal dados do usuário autenticado
     * @return DTO com as informações do endereço atualizado
     * @throws ResponseStatusException se não autenticado (401), não encontrado (404) ou acesso negado (403)
     */
    @Transactional
    public AddressResponseDTO updateAddress(Long id, AddressUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + id
                ));

        validateAddressAccessPermission(address, principal);

        if (!request.hasUpdates()) {
            return AddressResponseDTO.fromEntity(address);
        }

        if (request.zipCode() != null && !request.zipCode().isBlank()) {
            address.setZipCode(request.zipCode());
        }
        if (request.state() != null && !request.state().isBlank()) {
            address.setState(request.state());
        }
        if (request.city() != null && !request.city().isBlank()) {
            address.setCity(request.city());
        }
        if (request.number() != null && !request.number().isBlank()) {
            address.setNumber(request.number());
        }
        if (request.country() != null && !request.country().isBlank()) {
            address.setCountry(request.country());
        }

        Address updatedAddress = addressRepository.save(address);
        return AddressResponseDTO.fromEntity(updatedAddress);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private void validateAddressAccessPermission(Address address, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EMPLOYEE_NOT_FOUND));

            boolean matchesEnterprise = enterpriseRepository.findAllByIdAddress(address.getId())
                    .stream()
                    .anyMatch(enterprise -> Objects.equals(enterprise.getId(), employee.getIdEnterprise()));

            if (matchesEnterprise) {
                return;
            }

            boolean matchesFarm = farmRepository.findAllByIdAddress(address.getId())
                    .stream()
                    .anyMatch(farm -> Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise()));

            if (matchesFarm) {
                return;
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ACCESS_DENIED_ADDRESS);
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, FARM_OWNER_NOT_FOUND));

            boolean matchesFarm = farmRepository.findAllByIdAddress(address.getId())
                    .stream()
                    .anyMatch(farm -> Objects.equals(farm.getId(), owner.getIdFarm()));

            if (matchesFarm) {
                return;
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ACCESS_DENIED_ADDRESS);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para acessar este endereço");
    }
}