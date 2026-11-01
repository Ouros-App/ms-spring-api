package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.dto.stategoal.StateGoalTaskRequestDTO;
import jakarta.validation.Validation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateGoalTaskRequestDTOTest {
    @Test void deveAceitarMetaDescritivaSemAlvoNumericoESanitizarTextos() {
        var start = LocalDate.of(2026,11,1);
        var request = new StateGoalTaskRequestDTO(" Manutenção "," Orientações ",start,start);
        assertEquals("Manutenção",request.title()); assertEquals("Orientações",request.description());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            assertTrue(factory.getValidator().validate(new StateGoalTaskRequestDTO("Meta",null,start,null)).isEmpty());
            assertFalse(factory.getValidator().validate(new StateGoalTaskRequestDTO(null,null,null,null)).isEmpty());
            assertFalse(factory.getValidator().validate(new StateGoalTaskRequestDTO(" ",null,start,null)).isEmpty());
            assertFalse(factory.getValidator().validate(new StateGoalTaskRequestDTO("Meta",null,start,start.minusDays(1))).isEmpty());
        }
    }
}
