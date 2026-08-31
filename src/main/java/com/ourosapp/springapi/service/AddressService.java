package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.AddressResponseDTO;
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
     * @param request dados do endereço a ser criado
     * @return DTO com os dados do endereço cadastrado incluindo o ID gerado
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
     * Busca as informações de um endereço a partir do seu identificador único.
     *
     * @param id identificador único do endereço
     * @return DTO com as informações do endereço encontrado
     * @throws ResponseStatusException HTTP 404 (Not Found) se o endereço não existir
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
     * Substitui integralmente os dados de um endereço existente por novos valores.
     *
     * @param id      identificador único do endereço a ser atualizado
     * @param request novos dados do endereço
     * @return DTO com os dados atualizados do endereço
     * @throws ResponseStatusException HTTP 404 (Not Found) se o endereço não existir
     */
    @Transactional
    public AddressResponseDTO updateAddress(Long id, AddressRequestDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + id
                ));
        address.setZipCode(request.zipCode());
        address.setState(request.state());
        address.setCity(request.city());
        address.setNumber(request.number());
        address.setCountry(request.country());
        Address updatedAddress = addressRepository.save(address);
        return AddressResponseDTO.fromEntity(updatedAddress);
    }
}