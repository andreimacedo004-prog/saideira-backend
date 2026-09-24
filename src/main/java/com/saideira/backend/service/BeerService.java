package com.saideira.backend.service;

import com.saideira.backend.dto.CervejaResponse;
import com.saideira.backend.model.Beer;
import com.saideira.backend.repository.BeerRepository;
import com.saideira.backend.util.Normalizador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Catalogo de cervejas: busca e cadastro.
 *
 * Cadastrar uma cerveja que ja existe (mesmo nome e cervejaria, ignorando
 * maiusculas, acentos e espacos) devolve a existente em vez de duplicar.
 */
@Service
public class BeerService {

    /** Resultado do cadastro: a cerveja e se ela acabou de ser criada. */
    public record Cadastro(CervejaResponse cerveja, boolean criada) {}

    private final BeerRepository beerRepository;
    private final UserService userService;

    public BeerService(BeerRepository beerRepository, UserService userService) {
        this.beerRepository = beerRepository;
        this.userService = userService;
    }

    /**
     * Autocomplete. Quem digita "heineken" quer a Heineken no topo, e nao a
     * Amstel (que tambem casa, pela cervejaria): primeiro vem nome que comeca
     * com o texto, depois nome que contem o texto, e por ultimo o que so casou
     * pela cervejaria.
     */
    @Transactional(readOnly = true)
    public List<CervejaResponse> buscar(String busca) {
        String trecho = Normalizador.normalizar(busca);
        if (trecho.isEmpty()) {
            return beerRepository.findTop20ByOrderByNomeAsc().stream().map(CervejaResponse::de).toList();
        }
        return beerRepository.findTop50ByChaveBuscaContainingOrderByNomeAsc(trecho).stream()
            .sorted(Comparator.comparingInt((Beer b) -> relevancia(b, trecho)))
            .limit(20)
            .map(CervejaResponse::de)
            .toList();
    }

    private static int relevancia(Beer cerveja, String trecho) {
        String nome = Normalizador.normalizar(cerveja.getNome());
        if (nome.startsWith(trecho)) {
            return 0;
        }
        return nome.contains(trecho) ? 1 : 2;
    }

    @Transactional
    public Cadastro cadastrar(String nome, String estilo, String cervejaria, Long usuarioId) {
        String chave = chave(nome, cervejaria);

        return beerRepository.findByChaveBusca(chave)
            .map(existente -> new Cadastro(CervejaResponse.de(existente), false))
            .orElseGet(() -> {
                Beer nova = new Beer();
                nova.setNome(nome.trim());
                nova.setEstilo(vazioViraNulo(estilo));
                nova.setCervejaria(vazioViraNulo(cervejaria));
                nova.setChaveBusca(chave);
                nova.setCriadoPor(userService.buscarPorId(usuarioId));
                return new Cadastro(CervejaResponse.de(beerRepository.save(nova)), true);
            });
    }

    /** Mesmo formato usado no catalogo inicial (V2__cervejas_iniciais.sql). */
    public static String chave(String nome, String cervejaria) {
        return Normalizador.normalizar(nome) + "|" + Normalizador.normalizar(cervejaria);
    }

    private static String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
