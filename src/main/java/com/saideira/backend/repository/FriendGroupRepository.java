package com.saideira.backend.repository;

import com.saideira.backend.model.FriendGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendGroupRepository extends JpaRepository<FriendGroup, Long> {

    List<FriendGroup> findByMembrosId(Long usuarioId);

    Optional<FriendGroup> findByCodigoConvite(String codigoConvite);

    boolean existsByCodigoConvite(String codigoConvite);
}
