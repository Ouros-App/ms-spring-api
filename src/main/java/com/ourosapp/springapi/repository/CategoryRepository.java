package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade {@link Category}.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByIdTip(Long idTip);

    List<Category> findByIdIn(List<Long> ids);

    boolean existsByCategoryIgnoreCase(String category);
}
