package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.measurementimport.MeasurementImportRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeasurementImportRequestDTOTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void tearDown() {
        Validation.buildDefaultValidatorFactory().close();
    }

    @Test
    void rejectsEmptyBatch() {
        assertFalse(validator.validate(new MeasurementImportRequestDTO(null, null)).isEmpty());
        assertFalse(validator.validate(new MeasurementImportRequestDTO(Collections.emptyList(), null)).isEmpty());
    }

    @Test
    void rejectsMoreThan500RowsAcrossBothKinds() {
        assertFalse(validator.validate(new MeasurementImportRequestDTO(
                Collections.nCopies(250, validWater()),
                Collections.nCopies(251, validEnergy()))).isEmpty());
    }

    @Test
    void allowsNonemptyBatchOf500Rows() {
        assertTrue(validator.validate(new MeasurementImportRequestDTO(
                Collections.nCopies(499, validWater()),
                Collections.singletonList(validEnergy()))).isEmpty());
    }

    @Test
    void rejectsNullRows() {
        assertFalse(validator.validate(new MeasurementImportRequestDTO(
                Collections.singletonList(null), null)).isEmpty());
    }

    private static WaterRegistryRequestDTO validWater() {
        return new WaterRegistryRequestDTO(
                LocalDate.of(2025, 1, 1),
                BigDecimal.ONE,
                BigDecimal.ONE,
                null);
    }

    private static EnergyRegistryRequestDTO validEnergy() {
        return new EnergyRegistryRequestDTO(
                LocalDate.of(2025, 1, 1),
                BigDecimal.ONE,
                null);
    }
}
