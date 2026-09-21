package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.TipCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade de associação {@link TipCategory}.
 */
@Repository
public interface TipCategoryRepository extends JpaRepository<TipCategory, Long> {

    List<TipCategory> findByIdTip(Long idTip);

    List<TipCategory> findByIdTipIn(List<Long> tipIds);

    List<TipCategory> findByIdCategory(Long idCategory);

    boolean existsByIdTipAndIdCategory(Long idTip, Long idCategory);

    void deleteByIdTip(Long idTip);

    void deleteByIdTipAndIdCategory(Long idTip, Long idCategory);
}
