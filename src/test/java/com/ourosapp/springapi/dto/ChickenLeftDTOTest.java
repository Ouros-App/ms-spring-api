package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftRequestDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftResponseDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftUpdateDTO;
import com.ourosapp.springapi.entity.ChickenLeft;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para validação, serialização JSON e comportamentos dos DTOs de saída de aves.
 */
class ChickenLeftDTOTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** Verifica que um DTO de criação válido não produz violações de validação. */
    @Test
    @DisplayName("Deve validar com sucesso DTO de requisição válido")
    void deveValidarDtoDeRequisicaoValido() {
        ChickenLeftRequestDTO dto = new ChickenLeftRequestDTO(
                500,
                LocalDate.now(),
                1L
        );

        Set<ConstraintViolation<ChickenLeftRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    /** Verifica que o identificador da fazenda pode ser omitido no DTO de criação. */
    @Test
    @DisplayName("Deve validar com sucesso DTO de requisição válido sem id_farm (para produtor rural)")
    void deveValidarDtoDeRequisicaoValidoSemIdFarm() {
        ChickenLeftRequestDTO dto = new ChickenLeftRequestDTO(
                500,
                LocalDate.now(),
                null
        );

        Set<ConstraintViolation<ChickenLeftRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    /** Verifica as violações produzidas por campos obrigatórios ou inválidos. */
    @Test
    @DisplayName("Deve detectar violações quando campos obrigatórios forem nulos ou inválidos")
    void deveDetectarViolacoesEmCamposInvalidos() {
        ChickenLeftRequestDTO dto = new ChickenLeftRequestDTO(
                -10,
                null,
                -1L
        );

        Set<ConstraintViolation<ChickenLeftRequestDTO>> violations = validator.validate(dto);

        assertEquals(3, violations.size());
    }

    /** Verifica a violação produzida por uma data futura no DTO de criação. */
    @Test
    @DisplayName("Deve detectar erro de validação quando exitDate for data futura no ChickenLeftRequestDTO")
    void deveDetectarDataFuturaNoRequestDTO() {
        ChickenLeftRequestDTO dto = new ChickenLeftRequestDTO(
                500,
                LocalDate.now().plusDays(1),
                1L
        );

        Set<ConstraintViolation<ChickenLeftRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("A data de saída não pode ser uma data futura", violations.iterator().next().getMessage());
    }

    /** Verifica a violação produzida por quantidade zero ou negativa no DTO de criação. */
    @Test
    @DisplayName("Deve detectar erro de validação quando chickensCount for zero")
    void deveDetectarQuantidadeZeroNoRequestDTO() {
        ChickenLeftRequestDTO dto = new ChickenLeftRequestDTO(
                0,
                LocalDate.now(),
                1L
        );

        Set<ConstraintViolation<ChickenLeftRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("A quantidade de aves de saída deve ser maior que zero", violations.iterator().next().getMessage());
    }

    /** Verifica a desserialização dos campos em snake_case do DTO de criação. */
    @Test
    @DisplayName("Deve serializar e desserializar ChickenLeftRequestDTO com snake_case")
    void deveSerializarEDesserializarRequestDTO() throws JsonProcessingException {
        String json = """
                {
                    "chickens_count": 350,
                    "exit_date": "2026-09-20",
                    "id_farm": 5
                }
                """;

        ChickenLeftRequestDTO dto = objectMapper.readValue(json, ChickenLeftRequestDTO.class);

        assertNotNull(dto);
        assertEquals(350, dto.chickensCount());
        assertEquals(LocalDate.of(2026, 9, 20), dto.exitDate());
        assertEquals(5L, dto.idFarm());
    }

    /** Verifica a conversão de uma entidade para o DTO de resposta. */
    @Test
    @DisplayName("Deve converter entidade ChickenLeft em ChickenLeftResponseDTO")
    void deveConverterEntidadeEmResponseDTO() {
        ChickenLeft entity = ChickenLeft.builder()
                .id(100L)
                .chickensCount(400)
                .exitDate(LocalDate.of(2026, 9, 20))
                .idFarm(2L)
                .build();

        ChickenLeftResponseDTO response = ChickenLeftResponseDTO.fromEntity(entity);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(400, response.chickensCount());
        assertEquals(LocalDate.of(2026, 9, 20), response.exitDate());
        assertEquals(2L, response.idFarm());
    }

    /** Verifica a rejeição de uma entidade nula durante a conversão para DTO. */
    @Test
    @DisplayName("Deve lançar NullPointerException ao passar entidade nula para fromEntity")
    void deveLancarExcecaoParaEntidadeNula() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> ChickenLeftResponseDTO.fromEntity(null)
        );
        assertEquals("ChickenLeft não pode ser nulo", ex.getMessage());
    }

    /** Verifica os métodos gerados e os construtores da entidade de saída de aves. */
    @Test
    @DisplayName("Deve testar getters, setters, builder e toString da entidade ChickenLeft")
    void deveTestarEntidadeChickenLeft() {
        LocalDate date = LocalDate.of(2026, 9, 20);

        ChickenLeft entity = new ChickenLeft();
        entity.setId(1L);
        entity.setChickensCount(500);
        entity.setExitDate(date);
        entity.setIdFarm(10L);

        assertEquals(1L, entity.getId());
        assertEquals(500, entity.getChickensCount());
        assertEquals(date, entity.getExitDate());
        assertEquals(10L, entity.getIdFarm());
        assertTrue(entity.toString().contains("500"));

        ChickenLeft allArgs = new ChickenLeft(2L, 600, date, 20L);
        assertEquals(2L, allArgs.getId());
        assertEquals(600, allArgs.getChickensCount());
        assertEquals(20L, allArgs.getIdFarm());
    }

    /** Verifica a detecção de campos presentes no DTO de atualização. */
    @Test
    @DisplayName("Deve testar hasUpdates() do ChickenLeftUpdateDTO")
    void deveTestarHasUpdatesNoUpdateDTO() {
        ChickenLeftUpdateDTO vazio = new ChickenLeftUpdateDTO(null, null);
        assertFalse(vazio.hasUpdates());

        ChickenLeftUpdateDTO apenasData = new ChickenLeftUpdateDTO(null, LocalDate.now());
        assertTrue(apenasData.hasUpdates());

        ChickenLeftUpdateDTO apenasQuantidade = new ChickenLeftUpdateDTO(100, null);
        assertTrue(apenasQuantidade.hasUpdates());

        ChickenLeftUpdateDTO completo = new ChickenLeftUpdateDTO(100, LocalDate.now());
        assertTrue(completo.hasUpdates());
    }

    /** Verifica a violação produzida por quantidade negativa no DTO de atualização. */
    @Test
    @DisplayName("Deve detectar erro de validação quando quantidade for negativa no ChickenLeftUpdateDTO")
    void deveDetectarQuantidadeNegativaNoUpdateDTO() {
        ChickenLeftUpdateDTO dto = new ChickenLeftUpdateDTO(-50, null);
        Set<ConstraintViolation<ChickenLeftUpdateDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    /** Verifica a violação produzida por uma data futura no DTO de atualização. */
    @Test
    @DisplayName("Deve detectar erro de validação quando exitDate for data futura no ChickenLeftUpdateDTO")
    void deveDetectarDataFuturaNoUpdateDTO() {
        ChickenLeftUpdateDTO dto = new ChickenLeftUpdateDTO(null, LocalDate.now().plusDays(1));
        Set<ConstraintViolation<ChickenLeftUpdateDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A data de saída não pode ser uma data futura", violations.iterator().next().getMessage());
    }

    /** Verifica a desserialização dos campos em snake_case do DTO de atualização. */
    @Test
    @DisplayName("Deve serializar e desserializar ChickenLeftUpdateDTO com snake_case")
    void deveSerializarEDesserializarUpdateDTO() throws JsonProcessingException {
        String json = """
                {
                    "chickens_count": 600,
                    "exit_date": "2026-09-21"
                }
                """;

        ChickenLeftUpdateDTO dto = objectMapper.readValue(json, ChickenLeftUpdateDTO.class);
        assertNotNull(dto);
        assertEquals(600, dto.chickensCount());
        assertEquals(LocalDate.of(2026, 9, 21), dto.exitDate());
    }
}
