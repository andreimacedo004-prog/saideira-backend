package com.saideira.backend.service;

import com.saideira.backend.model.Beer;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.BeerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeerServiceTest {

    @Mock
    private BeerRepository beerRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BeerService beerService;

    @Test
    @DisplayName("Chave ignora caixa, acento e espacos, e junta nome com cervejaria")
    void chaveNormalizada() {
        assertThat(BeerService.chave("  Colorado  Appia ", "COLORADO")).isEqualTo("colorado appia|colorado");
        assertThat(BeerService.chave("Itaipava", "Grupo Petrópolis")).isEqualTo("itaipava|grupo petropolis");
        assertThat(BeerService.chave("Chopp da casa", null)).isEqualTo("chopp da casa|");
    }

    @Test
    @DisplayName("Cadastrar cerveja que ja existe devolve a existente, sem duplicar")
    void naoDuplica() {
        Beer existente = new Beer();
        existente.setId(3L);
        existente.setNome("Heineken");
        existente.setCervejaria("Heineken");
        when(beerRepository.findByChaveBusca("heineken|heineken")).thenReturn(Optional.of(existente));

        BeerService.Cadastro cadastro = beerService.cadastrar("HEINEKEN ", null, "heineken", 1L);

        assertThat(cadastro.criada()).isFalse();
        assertThat(cadastro.cerveja().id()).isEqualTo(3L);
        verify(beerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cerveja nova e salva com a chave e quem cadastrou")
    void cadastraNova() {
        User ana = new User();
        ana.setId(1L);
        when(beerRepository.findByChaveBusca("hocus pocus magic trap|hocus pocus")).thenReturn(Optional.empty());
        when(userService.buscarPorId(1L)).thenReturn(ana);
        when(beerRepository.save(any(Beer.class))).thenAnswer(inv -> inv.getArgument(0));

        BeerService.Cadastro cadastro = beerService.cadastrar("Hocus Pocus Magic Trap", "IPA", "Hocus Pocus", 1L);

        assertThat(cadastro.criada()).isTrue();
        assertThat(cadastro.cerveja().estilo()).isEqualTo("IPA");
    }

    private Beer cerveja(long id, String nome, String cervejaria) {
        Beer b = new Beer();
        b.setId(id);
        b.setNome(nome);
        b.setCervejaria(cervejaria);
        return b;
    }

    @Test
    @DisplayName("Busca vazia lista o catalogo; busca com texto procura pela chave normalizada")
    void busca() {
        when(beerRepository.findTop20ByOrderByNomeAsc()).thenReturn(List.of());
        when(beerRepository.findTop50ByChaveBuscaContainingOrderByNomeAsc("petropolis")).thenReturn(List.of());

        beerService.buscar("  ");
        beerService.buscar("Petrópolis");

        verify(beerRepository).findTop20ByOrderByNomeAsc();
        verify(beerRepository).findTop50ByChaveBuscaContainingOrderByNomeAsc("petropolis");
    }

    @Test
    @DisplayName("Quem busca 'heineken' ve a Heineken antes das outras da mesma cervejaria")
    void buscaPorRelevancia() {
        // O banco devolve em ordem alfabetica
        when(beerRepository.findTop50ByChaveBuscaContainingOrderByNomeAsc("heineken")).thenReturn(List.of(
            cerveja(1, "Amstel", "Heineken"),
            cerveja(2, "Devassa", "Heineken"),
            cerveja(3, "Heineken", "Heineken"),
            cerveja(4, "Sol", "Heineken")
        ));

        assertThat(beerService.buscar("heineken")).extracting(c -> c.nome())
            .containsExactly("Heineken", "Amstel", "Devassa", "Sol");
    }
}
