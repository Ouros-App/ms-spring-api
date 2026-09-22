package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.dto.category.CategoryRequestDTO;
import com.ourosapp.springapi.dto.category.CategoryResponseDTO;
import com.ourosapp.springapi.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs do domínio de Categoria.
 */
class CategoryDtoTest {

    @Test
    @DisplayName("CategoryRequestDTO - Deve sanitizar espaços no nome da categoria")
    void deveSanitizarEspacosEmCategoryRequestDTO() {
        CategoryRequestDTO request = new CategoryRequestDTO("   Manejo de Ambiência   ", 1L);
        assertEquals("Manejo de Ambiência", request.category());
        assertEquals(1L, request.idTip());
    }

    @Test
    @DisplayName("CategoryResponseDTO - Deve instanciar corretamente a partir da entidade Category")
    void deveInstanciarCategoryResponseDTOFromEntity() {
        Category category = Category.builder()
                .id(10L)
                .category("Biosseguridade")
                
                .build();

        CategoryResponseDTO response = CategoryResponseDTO.fromEntity(category);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Biosseguridade", response.category());
        
    }

    @Test
    @DisplayName("CategoryResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadeNula() {
        assertNull(CategoryResponseDTO.fromEntity(null));
    }
}
