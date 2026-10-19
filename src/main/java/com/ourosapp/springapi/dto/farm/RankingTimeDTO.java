package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

/**
 * Objeto contendo o tempo restante até o próximo reset do ranking formatado para apresentação no mobile.
 *
 * @param days    quantidade de dias restantes formatada (ex: "2 dias", "1 dia") ou null se menor que 24 horas
 * @param hours   quantidade de horas restantes formatada (ex: "6 horas", "1 hora")
 * @param minutes quantidade de minutos restantes formatada (ex: "8 minutos", "1 minuto")
 * @param seconds quantidade de segundos restantes formatada (ex: "45 segundos") ou null quando houver dias/horas/minutos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Tempo restante estruturado para apresentação amigável no aplicativo mobile")
public record RankingTimeDTO(

        @Schema(description = "Dias restantes até o próximo reset do ranking (omitido se menor que 24h)", example = "2 dias")
        @JsonProperty("days")
        @JsonAlias({"dias", "day"})
        String days,

        @Schema(description = "Horas restantes até o próximo reset do ranking", example = "6 horas")
        @JsonProperty("hours")
        @JsonAlias({"horas", "hour"})
        String hours,

        @Schema(description = "Minutos restantes até o próximo reset do ranking", example = "8 minutos")
        @JsonProperty("minutes")
        @JsonAlias({"minutos", "minute"})
        String minutes,

        @Schema(description = "Segundos restantes (apresentado apenas quando faltar menos de 1 minuto)", example = "45 segundos")
        @JsonProperty("seconds")
        @JsonAlias({"segundos", "second"})
        String seconds
) {

    public RankingTimeDTO(String days, String hours, String minutes) {
        this(days, hours, minutes, null);
    }

    /**
     * Constrói uma instância de {@link RankingTimeDTO} a partir de uma duração.
     *
     * @param duration duração calculada até o próximo reset
     * @return DTO estruturado com os textos amigáveis em português e campos padronizados em inglês
     */
    public static RankingTimeDTO fromDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return new RankingTimeDTO(null, "0 horas", "0 minutos", "0 segundos");
        }

        long totalDays = duration.toDays();
        long totalHours = duration.toHoursPart();
        long totalMinutes = duration.toMinutesPart();
        long totalSeconds = duration.toSecondsPart();

        String days = totalDays > 0 ? (totalDays == 1 ? "1 dia" : totalDays + " dias") : null;
        String hours = totalHours == 1 ? "1 hora" : totalHours + " horas";
        String minutes = totalMinutes == 1 ? "1 minuto" : totalMinutes + " minutos";
        String seconds = (totalDays == 0 && totalHours == 0 && totalMinutes == 0)
                ? (totalSeconds == 1 ? "1 segundo" : totalSeconds + " segundos")
                : null;

        return new RankingTimeDTO(days, hours, minutes, seconds);
    }

    /**
     * Retorna a representação em texto corrido tradicional (ex: "2 dias, 6 horas e 8 minutos").
     *
     * @return texto formatado legível
     */
    public String toHumanReadable() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (days != null && !days.isBlank()) {
            parts.add(days);
        }
        if (hours != null && !hours.isBlank() && !"0 horas".equals(hours)) {
            parts.add(hours);
        }
        if (minutes != null && !minutes.isBlank() && !"0 minutos".equals(minutes)) {
            parts.add(minutes);
        }
        if (seconds != null && !seconds.isBlank() && !"0 segundos".equals(seconds)) {
            parts.add(seconds);
        }
        if (parts.isEmpty()) {
            if (seconds != null && !seconds.isBlank()) {
                return seconds;
            }
            if (minutes != null && !minutes.isBlank()) {
                return minutes;
            }
            if (hours != null && !hours.isBlank()) {
                return hours;
            }
            return "0 minutos";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        } else if (parts.size() == 2) {
            return parts.get(0) + " e " + parts.get(1);
        } else {
            return String.join(", ", parts.subList(0, parts.size() - 1)) + " e " + parts.get(parts.size() - 1);
        }
    }

    @JsonIgnore
    public String dias() {
        return days;
    }

    @JsonIgnore
    public String horas() {
        return hours;
    }

    @JsonIgnore
    public String minutos() {
        return minutes;
    }

    @JsonIgnore
    public String segundos() {
        return seconds;
    }
}
