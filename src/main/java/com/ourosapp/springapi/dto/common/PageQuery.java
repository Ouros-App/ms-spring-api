package com.ourosapp.springapi.dto.common;

import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PageQuery(int page, int size, String search, String sort, String direction) {
    public PageQuery {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Página deve ser não negativa e tamanho deve estar entre 1 e 100; offset máximo: 2147483647");
        }
        search = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        sort = sort == null || sort.isBlank() ? "id" : sort;
        direction = direction == null ? "desc" : direction.toLowerCase(Locale.ROOT);
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direção deve ser asc ou desc");
        }
    }
    public PageRequest pageable() { return PageRequest.of(page, size); }
    public String orderBy(Map<String, String> fields, String id) {
        String field = fields.get(sort);
        if (field == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Campo de ordenação inválido. Permitidos: " + fields.keySet());
        return field + " " + direction + (field.equals(id) ? "" : ", " + id + " " + direction);
    }
    public String pattern() {
        return "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
