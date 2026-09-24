package com.saideira.backend.repository;

import com.saideira.backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.autor
        WHERE c.checkIn.id = :checkInId
        ORDER BY c.criadoEm ASC, c.id ASC
        """)
    List<Comment> findDoCheckIn(@Param("checkInId") Long checkInId);

    /** Cada linha: [checkInId, total de comentarios] */
    @Query("""
        SELECT c.checkIn.id, COUNT(c)
        FROM Comment c
        WHERE c.checkIn.id IN :checkInIds
        GROUP BY c.checkIn.id
        """)
    List<Object[]> contarPorCheckIn(@Param("checkInIds") Collection<Long> checkInIds);
}
