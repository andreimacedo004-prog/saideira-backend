package com.saideira.backend.repository;

import com.saideira.backend.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /**
     * Todos os check-ins de um desafio, do mais recente para o mais antigo.
     * Serve tanto o feed quanto o calculo do ranking (que precisa do historico inteiro).
     * Amigos e cervejas vem em lote pelo default_batch_fetch_size.
     */
    @Query("""
        SELECT c FROM CheckIn c
        JOIN FETCH c.autor
        WHERE c.desafio.id = :desafioId
        ORDER BY c.feitoEm DESC, c.id DESC
        """)
    List<CheckIn> findDoDesafio(@Param("desafioId") Long desafioId);

    /**
     * Check-in do mesmo usuario, no mesmo desafio, perto demais do horario informado.
     * Limites exclusivos: exatamente 2h depois ja vale.
     */
    @Query("""
        SELECT c FROM CheckIn c
        WHERE c.autor.id = :autorId
          AND c.desafio.id = :desafioId
          AND c.feitoEm > :depoisDe
          AND c.feitoEm < :antesDe
        ORDER BY c.feitoEm DESC
        LIMIT 1
        """)
    Optional<CheckIn> findConflitante(
        @Param("autorId") Long autorId,
        @Param("desafioId") Long desafioId,
        @Param("depoisDe") LocalDateTime depoisDe,
        @Param("antesDe") LocalDateTime antesDe
    );
}
