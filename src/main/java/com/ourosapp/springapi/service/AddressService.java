package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.address.AddressRequestDTO;
import com.ourosapp.springapi.dto.address.AddressResponseDTO;
import com.ourosapp.springapi.dto.address.AddressUpdateDTO;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.repository.AddressRepository;
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
     * Busca um endereço pelo seu identificador único.
     *
     * @param id identificador do endereço
     * @return DTO com as informações do endereço encontrado
     * @throws ResponseStatusException se o endereço não for encontrado
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + id
                ));
        return AddressResponseDTO.fromEntity(address);
    }

    /**
     * Atualiza parcialmente os dados de um endereço existente.
     *
     * @param id      identificador do endereço
     * @param request dados para atualização (apenas os campos preenchidos serão atualizados)
     * @return DTO com as informações do endereço atualizado
     * @throws ResponseStatusException se o endereço não for encontrado
     */
    @Transactional
    public AddressResponseDTO updateAddress(Long id, AddressUpdateDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + id
                ));

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
}