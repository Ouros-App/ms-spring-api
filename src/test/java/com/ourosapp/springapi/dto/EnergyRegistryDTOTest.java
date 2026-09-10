package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryUpdateDTO;
import com.ourosapp.springapi.entity.EnergyRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para validação, serialização JSON e comportamentos dos DTOs de registro de energia.
 */
class EnergyRegistryDTOTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Deve validar com sucesso DTO de requisição válido")
    void deveValidarDtoDeRequisicaoValido() {
        EnergyRegistryRequestDTO dto = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("150.00"),
                1L
        );

        Set<ConstraintViolation<EnergyRegistryRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Deve validar com sucesso DTO de requisição válido sem id_farm (para produtor rural)")
    void deveValidarDtoDeRequisicaoValidoSemIdFarm() {
        EnergyRegistryRequestDTO dto = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("150.00"),
                null
        );

        Set<ConstraintViolation<EnergyRegistryRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Deve detectar violações quando campos obrigatórios forem nulos ou inválidos")
    void deveDetectarViolacoesEmCamposInvalidos() {
        EnergyRegistryRequestDTO dto = new EnergyRegistryRequestDTO(
                null,
                new BigDecimal("-5.00"),
                -1L
        );

        Set<ConstraintViolation<EnergyRegistryRequestDTO>> violations = validator.validate(dto);

        assertEquals(3, violations.size());
    }

    @Test
    @DisplayName("Deve serializar e desserializar EnergyRegistryRequestDTO com snake_case")
    void deveSerializarEDesserializarRequestDTO() throws JsonProcessingException {
        String json = """
                {
                    "registration_date": "2026-09-09",
                    "energy_consumption": 320.50,
                    "id_farm": 5
                }
                """;

        EnergyRegistryRequestDTO dto = objectMapper.readValue(json, EnergyRegistryRequestDTO.class);

        assertNotNull(dto);
        assertEquals(LocalDate.of(2026, 9, 9), dto.registrationDate());
        assertEquals(new BigDecimal("320.50"), dto.energyConsumption());
        assertEquals(5L, dto.idFarm());
    }

    @Test
    @DisplayName("Deve converter entidade EnergyRegistry em EnergyRegistryResponseDTO")
    void deveConverterEntidadeEmResponseDTO() {
        EnergyRegistry entity = EnergyRegistry.builder()
                .id(100L)
                .registrationDate(LocalDate.of(2026, 9, 9))
                .energyConsumption(new BigDecimal("500.00"))
                .idFarm(2L)
                .build();

        EnergyRegistryResponseDTO response = EnergyRegistryResponseDTO.fromEntity(entity);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(LocalDate.of(2026, 9, 9), response.registrationDate());
        assertEquals(new BigDecimal("500.00"), response.energyConsumption());
        assertEquals(2L, response.idFarm());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao passar entidade nula para daEntity")
    void deveLancarExcecaoParaEntidadeNula() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> EnergyRegistryResponseDTO.fromEntity(null)
        );
        assertEquals("EnergyRegistry não pode ser nulo", ex.getMessage());
    }

    @Test
    @DisplayName("Deve testar getters, setters, builder e toString da entidade EnergyRegistry")
    void deveTestarEntidadeEnergyRegistry() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        BigDecimal consumption = new BigDecimal("750.25");

        EnergyRegistry entity = new EnergyRegistry();
        entity.setId(1L);
        entity.setRegistrationDate(date);
        entity.setEnergyConsumption(consumption);
        entity.setIdFarm(10L);

        assertEquals(1L, entity.getId());
        assertEquals(date, entity.getRegistrationDate());
        assertEquals(consumption, entity.getEnergyConsumption());
        assertEquals(10L, entity.getIdFarm());
        assertTrue(entity.toString().contains("750.25"));

        EnergyRegistry allArgs = new EnergyRegistry(2L, date, consumption, 20L);
        assertEquals(2L, allArgs.getId());
        assertEquals(20L, allArgs.getIdFarm());
    }

    @Test
    @DisplayName("Deve testar hasUpdates() do EnergyRegistryUpdateDTO")
    void deveTestarHasUpdatesNoUpdateDTO() {
        EnergyRegistryUpdateDTO vazio = new EnergyRegistryUpdateDTO(null, null);
        assertFalse(vazio.hasUpdates());

        EnergyRegistryUpdateDTO apenasData = new EnergyRegistryUpdateDTO(LocalDate.now(), null);
        assertTrue(apenasData.hasUpdates());

        EnergyRegistryUpdateDTO apenasConsumo = new EnergyRegistryUpdateDTO(null, new BigDecimal("100.00"));
        assertTrue(apenasConsumo.hasUpdates());

        EnergyRegistryUpdateDTO completo = new EnergyRegistryUpdateDTO(LocalDate.now(), new BigDecimal("100.00"));
        assertTrue(completo.hasUpdates());
    }

    @Test
    @DisplayName("Deve detectar erro de validação quando consumo for negativo no EnergyRegistryUpdateDTO")
    void deveDetectarConsumoNegativoNoUpdateDTO() {
        EnergyRegistryUpdateDTO dto = new EnergyRegistryUpdateDTO(null, new BigDecimal("-50.00"));
        Set<ConstraintViolation<EnergyRegistryUpdateDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("Deve serializar e desserializar EnergyRegistryUpdateDTO com snake_case")
    void deveSerializarEDesserializarUpdateDTO() throws JsonProcessingException {
        String json = """
                {
                    "registration_date": "2026-09-10",
                    "energy_consumption": 480.00
                }
                """;

        EnergyRegistryUpdateDTO dto = objectMapper.readValue(json, EnergyRegistryUpdateDTO.class);
        assertNotNull(dto);
        assertEquals(LocalDate.of(2026, 9, 10), dto.registrationDate());
        assertEquals(new BigDecimal("480.00"), dto.energyConsumption());
    }
}
