package com.saideira.backend.repository;

import com.saideira.backend.model.Beer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeerRepository extends JpaRepository<Beer, Long> {

    Optional<Beer> findByChaveBusca(String chaveBusca);

    // A chave junta nome e cervejaria, entao buscar "ambev" tambem funciona.
    // Traz 50 para o service reordenar por relevancia e devolver as 20 melhores.
    List<Beer> findTop50ByChaveBuscaContainingOrderByNomeAsc(String trecho);

    List<Beer> findTop20ByOrderByNomeAsc();
}
