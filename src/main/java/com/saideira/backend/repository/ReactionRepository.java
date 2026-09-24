package com.saideira.backend.repository;

import com.saideira.backend.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByCheckInIdAndUsuarioIdAndTipo(Long checkInId, Long usuarioId, Reaction.Tipo tipo);

    /**
     * Resumo das reacoes de varios check-ins de uma vez (uma consulta para o feed todo).
     * Cada linha: [checkInId, tipo, total, reagiu(0/1 do usuario logado)]
     */
    @Query("""
        SELECT r.checkIn.id, r.tipo, COUNT(r),
               SUM(CASE WHEN r.usuario.id = :usuarioId THEN 1 ELSE 0 END)
        FROM Reaction r
        WHERE r.checkIn.id IN :checkInIds
        GROUP BY r.checkIn.id, r.tipo
        """)
    List<Object[]> resumoPorCheckIn(
        @Param("checkInIds") Collection<Long> checkInIds,
        @Param("usuarioId") Long usuarioId
    );
}
