package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Adm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmRepository extends JpaRepository<Adm, Long> {
    Optional<Adm> findByEmail(String email);
    boolean existsByEmail(String email);
}
