package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A galera: um grupo fechado de amigos (ex: "Resenha da Faculdade").
 * Desafios, feed e ranking existem sempre dentro de um grupo, nunca em publico.
 */
@Entity
@Table(name = "friend_groups")
@Getter
@Setter
@NoArgsConstructor
public class FriendGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private User criadoPor;

    /**
     * Codigo do link de convite (ex: "a7f3k9qp").
     * Quem abre o link manda esse codigo para POST /api/grupos/entrar e vira membro.
     */
    @Column(nullable = false, unique = true, length = 32)
    private String codigoConvite;

    @ManyToMany
    @JoinTable(
        name = "friend_group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> membros = new ArrayList<>();

    public boolean temMembro(Long usuarioId) {
        return membros.stream().anyMatch(m -> m.getId().equals(usuarioId));
    }
}
