package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade {@link Review}.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByIdTip(Long idTip);

    List<Review> findByIdTipIn(List<Long> tipIds);

    long countByIdTip(Long idTip);

    void deleteByIdTip(Long idTip);
}
