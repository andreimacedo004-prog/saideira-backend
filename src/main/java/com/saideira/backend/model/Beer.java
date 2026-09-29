package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "beers")
@Getter
@Setter
@NoArgsConstructor
public class Beer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String estilo;     // opcional (ex: IPA, Pilsen)
    private String cervejaria; // opcional

    /**
     * nome + cervejaria normalizados (ex: "colorado appia|colorado").
     * Unico no banco: e o que impede "Heineken" e "heineken " de virarem
     * duas cervejas — e, com isso, dois bonus de cerveja nova.
     * Tambem e onde a busca procura.
     */
    @Column(nullable = false, unique = true)
    private String chaveBusca;

    // Nulo nas cervejas que vieram no catalogo inicial
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private User criadoPor;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
