package com.saideira.backend.repository;

import com.saideira.backend.model.AjustePontos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AjustePontosRepository extends JpaRepository<AjustePontos, Long> {

    @Query("""
        SELECT a FROM AjustePontos a
        JOIN FETCH a.usuario
        WHERE a.desafio.id = :desafioId
        ORDER BY a.criadoEm ASC, a.id ASC
        """)
    List<AjustePontos> findDoDesafio(@Param("desafioId") Long desafioId);
}
