package com.saideira.backend.repository;

import com.saideira.backend.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByGrupoIdOrderByDataInicioDesc(Long grupoId);

    /** Desafios de todos os grupos de que o usuario participa (tela inicial do app). */
    @Query("""
        SELECT c FROM Challenge c
        JOIN FETCH c.grupo g
        WHERE EXISTS (SELECT 1 FROM FriendGroup fg JOIN fg.membros m WHERE fg = g AND m.id = :usuarioId)
        ORDER BY c.dataFim DESC
        """)
    List<Challenge> findDoUsuario(@Param("usuarioId") Long usuarioId);
}
