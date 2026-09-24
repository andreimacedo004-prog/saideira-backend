package com.saideira.backend.dto;

import com.saideira.backend.model.FriendGroup;

import java.util.List;

public record FriendGroupResponse(
    Long id,
    String nome,
    Long criadoPorId,
    String codigoConvite,
    List<UsuarioResumo> membros
) {
    public static FriendGroupResponse de(FriendGroup g) {
        return new FriendGroupResponse(
            g.getId(),
            g.getNome(),
            g.getCriadoPor().getId(),
            g.getCodigoConvite(),
            g.getMembros().stream().map(UsuarioResumo::de).toList()
        );
    }
}
