package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.AddressResponseDTO;
import com.ourosapp.springapi.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de endereços.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços")
@SecurityRequirement(name = "BearerAuth")
public class AddressController {

    private final AddressService addressService;

    /**
     * Endpoint para cadastrar um novo endereço.
     *
     * @param request corpo da requisição contendo os dados do novo endereço
     * @return resposta HTTP com status 201 (Created) e o DTO do endereço cadastrado
     */
    @Operation(summary = "Cadastrar endereço", description = "Cadastra um novo endereço no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Endereço cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @PostMapping
    public ResponseEntity<AddressResponseDTO> createAddress(@RequestBody @Valid AddressRequestDTO request) {
        AddressResponseDTO response = addressService.createAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para buscar um endereço específico através do seu ID.
     *
     * @param id identificador único do endereço
     * @return resposta HTTP com status 200 (OK) e o DTO do endereço encontrado
     */
    @Operation(summary = "Buscar endereço por ID", description = "Retorna as informações de um endereço específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> getAddressById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }

    /**
     * Endpoint para substituir integralmente todas as informações de um endereço existente.
     *
     * @param id      identificador único do endereço a ser atualizado
     * @param request corpo da requisição com os novos dados do endereço
     * @return resposta HTTP com status 200 (OK) e o DTO do endereço atualizado
     */
    @Operation(summary = "Atualizar endereço", description = "Substitui integralmente todas as informações de um endereço existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> updateAddress(
            @PathVariable Long id,
            @RequestBody @Valid AddressRequestDTO request
    ) {
        return ResponseEntity.ok(addressService.updateAddress(id, request));
    }
}