package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link RankingTimeDTO} garantindo 100% de cobertura de código e branches.
 */
class RankingTimeDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve cobrir construtor de 3 argumentos")
    void deveCobrirConstrutorDeTresArgumentos() {
        var dto = new RankingTimeDTO("1 dia", "2 horas", "3 minutos");
        assertEquals("1 dia", dto.days());
        assertEquals("2 horas", dto.hours());
        assertEquals("3 minutos", dto.minutes());
        assertNull(dto.seconds());
    }

    @Test
    @DisplayName("Deve retornar tempo zerado quando duração for nula, negativa ou zero")
    void deveRetornarTempoZeradoQuandoDuracaoInvalidaOuZero() {
        var nulo = RankingTimeDTO.fromDuration(null);
        assertNull(nulo.days());
        assertEquals("0 horas", nulo.hours());
        assertEquals("0 minutos", nulo.minutes());
        assertEquals("0 segundos", nulo.seconds());

        var negativo = RankingTimeDTO.fromDuration(Duration.ofSeconds(-10));
        assertNull(negativo.days());
        assertEquals("0 horas", negativo.hours());
        assertEquals("0 minutos", negativo.minutes());
        assertEquals("0 segundos", negativo.seconds());

        var zero = RankingTimeDTO.fromDuration(Duration.ZERO);
        assertNull(zero.days());
        assertEquals("0 horas", zero.hours());
        assertEquals("0 minutos", zero.minutes());
        assertEquals("0 segundos", zero.seconds());
    }

    @Test
    @DisplayName("Deve formatar singular corretamente (1 dia, 1 hora, 1 minuto)")
    void deveFormatarSingularCorretamente() {
        var duration = Duration.ofDays(1).plusHours(1).plusMinutes(1).plusSeconds(1);
        var dto = RankingTimeDTO.fromDuration(duration);
        assertEquals("1 dia", dto.days());
        assertEquals("1 hora", dto.hours());
        assertEquals("1 minuto", dto.minutes());
        assertNull(dto.seconds());
    }

    @Test
    @DisplayName("Deve formatar plural corretamente (2 dias, 5 horas, 10 minutos)")
    void deveFormatarPluralCorretamente() {
        var duration = Duration.ofDays(2).plusHours(5).plusMinutes(10);
        var dto = RankingTimeDTO.fromDuration(duration);
        assertEquals("2 dias", dto.days());
        assertEquals("5 horas", dto.hours());
        assertEquals("10 minutos", dto.minutes());
        assertNull(dto.seconds());
    }

    @Test
    @DisplayName("Deve formatar segundos no singular e plural quando faltar menos de 1 minuto")
    void deveFormatarSegundosQuandoMenosDeUmMinuto() {
        var umSegundo = RankingTimeDTO.fromDuration(Duration.ofSeconds(1));
        assertNull(umSegundo.days());
        assertEquals("0 horas", umSegundo.hours());
        assertEquals("0 minutos", umSegundo.minutes());
        assertEquals("1 segundo", umSegundo.seconds());

        var dezSegundos = RankingTimeDTO.fromDuration(Duration.ofSeconds(10));
        assertNull(dezSegundos.days());
        assertEquals("0 horas", dezSegundos.hours());
        assertEquals("0 minutos", dezSegundos.minutes());
        assertEquals("10 segundos", dezSegundos.seconds());
    }

    @Test
    @DisplayName("Deve chamar métodos legados em português e métodos em inglês garantindo compatibilidade")
    void deveChamarMetodosLegadosEmPortugues() {
        var dto = new RankingTimeDTO("3 dias", "4 horas", "5 minutos", "6 segundos");
        assertEquals("3 dias", dto.days());
        assertEquals("4 horas", dto.hours());
        assertEquals("5 minutos", dto.minutes());
        assertEquals("6 segundos", dto.seconds());

        assertEquals("3 dias", dto.dias());
        assertEquals("4 horas", dto.horas());
        assertEquals("5 minutos", dto.minutos());
        assertEquals("6 segundos", dto.segundos());
    }

    @Test
    @DisplayName("Deve cobrir branches parciais de dias, horas e minutos isolados em fromDuration")
    void deveCobrirBranchesIsoladosEmFromDuration() {
        // totalDays == 0 && totalHours == 0 && totalMinutes > 0
        var soMinutos = RankingTimeDTO.fromDuration(Duration.ofMinutes(15).plusSeconds(30));
        assertNull(soMinutos.days());
        assertEquals("0 horas", soMinutos.hours());
        assertEquals("15 minutos", soMinutos.minutes());
        assertNull(soMinutos.seconds());

        // totalDays == 0 && totalHours > 0 && totalMinutes == 0
        var soHoras = RankingTimeDTO.fromDuration(Duration.ofHours(3).plusSeconds(30));
        assertNull(soHoras.days());
        assertEquals("3 horas", soHoras.hours());
        assertEquals("0 minutos", soHoras.minutes());
        assertNull(soHoras.seconds());

        // totalDays > 0 && totalHours == 0 && totalMinutes == 0
        var soDias = RankingTimeDTO.fromDuration(Duration.ofDays(3).plusSeconds(30));
        assertEquals("3 dias", soDias.days());
        assertEquals("0 horas", soDias.hours());
        assertEquals("0 minutos", soDias.minutes());
        assertNull(soDias.seconds());
    }

    @Test
    @DisplayName("Deve testar toHumanReadable com 1, 2, 3 e 4 partes")
    void deveTestarToHumanReadableComDiferentesQuantidadesDePartes() {
        var umaParte = new RankingTimeDTO(null, "1 hora", null, null);
        assertEquals("1 hora", umaParte.toHumanReadable());

        var duasPartes = new RankingTimeDTO("1 dia", "2 horas", null, null);
        assertEquals("1 dia e 2 horas", duasPartes.toHumanReadable());

        var tresPartes = new RankingTimeDTO("1 dia", "2 horas", "3 minutos", null);
        assertEquals("1 dia, 2 horas e 3 minutos", tresPartes.toHumanReadable());

        var quatroPartes = new RankingTimeDTO("1 dia", "2 horas", "3 minutos", "4 segundos");
        assertEquals("1 dia, 2 horas, 3 minutos e 4 segundos", quatroPartes.toHumanReadable());
    }

    @Test
    @DisplayName("Deve testar toHumanReadable com campos vazios, nulos ou zerados caindo nos fallbacks")
    void deveTestarToHumanReadableFallbacks() {
        var comSegundosFallback = new RankingTimeDTO("", "0 horas", "0 minutos", "45 segundos");
        assertEquals("45 segundos", comSegundosFallback.toHumanReadable());

        var comMinutosFallback = new RankingTimeDTO(null, " ", "0 minutos", null);
        assertEquals("0 minutos", comMinutosFallback.toHumanReadable());

        var comHorasFallback = new RankingTimeDTO(null, "0 horas", null, null);
        assertEquals("0 horas", comHorasFallback.toHumanReadable());

        var totalmenteNuloOuBranco = new RankingTimeDTO("  ", " ", "", "");
        assertEquals("0 minutos", totalmenteNuloOuBranco.toHumanReadable());

        var comDiasEmBranco = new RankingTimeDTO("  ", "1 hora", " ", "0 segundos");
        assertEquals("1 hora", comDiasEmBranco.toHumanReadable());

        var comMinutosValidos = new RankingTimeDTO(null, "0 horas", "5 minutos", null);
        assertEquals("5 minutos", comMinutosValidos.toHumanReadable());

        var comHorasValidas = new RankingTimeDTO(null, "5 horas", "0 minutos", null);
        assertEquals("5 horas", comHorasValidas.toHumanReadable());
    }

    @Test
    @DisplayName("Deve testar equals, hashCode e toString do record")
    void deveTestarEqualsHashCodeToString() {
        var dto1 = new RankingTimeDTO("1 dia", "2 horas", "3 minutos", "4 segundos");
        var dto2 = new RankingTimeDTO("1 dia", "2 horas", "3 minutos", "4 segundos");
        var dto3 = new RankingTimeDTO("2 dias", "2 horas", "3 minutos", "4 segundos");

        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertTrue(dto1.toString().contains("RankingTimeDTO"));
    }
}
