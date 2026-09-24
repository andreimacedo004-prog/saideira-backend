package com.saideira.backend.service;

import com.saideira.backend.dto.FriendGroupResponse;
import com.saideira.backend.dto.UsuarioResumo;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.FriendGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendGroupServiceTest {

    @Mock
    private FriendGroupRepository friendGroupRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FriendGroupService friendGroupService;

    private User usuario(Long id) {
        User u = new User();
        u.setId(id);
        u.setNome("Usuario " + id);
        u.setEmail("usuario" + id + "@teste.com");
        return u;
    }

    private FriendGroup grupoCom(User... membros) {
        FriendGroup grupo = new FriendGroup();
        grupo.setId(10L);
        grupo.setNome("Resenha");
        grupo.setCriadoPor(membros[0]);
        grupo.setCodigoConvite("abc12345");
        for (User m : membros) {
            grupo.getMembros().add(m);
        }
        return grupo;
    }

    @Test
    @DisplayName("Criar grupo gera codigo de convite e ja coloca o criador dentro")
    void criaGrupoComConvite() {
        when(userService.buscarPorId(1L)).thenReturn(usuario(1L));
        when(friendGroupRepository.existsByCodigoConvite(anyString())).thenReturn(false);
        when(friendGroupRepository.save(any(FriendGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendGroupResponse grupo = friendGroupService.criar(1L, "  Resenha da Facul ");

        assertThat(grupo.nome()).isEqualTo("Resenha da Facul");
        assertThat(grupo.codigoConvite()).hasSize(8);
        assertThat(grupo.membros()).extracting(UsuarioResumo::id).containsExactly(1L);
    }

    @Test
    @DisplayName("Entrar pelo codigo (em qualquer caixa) adiciona o usuario ao grupo")
    void entraPorCodigo() {
        when(friendGroupRepository.findByCodigoConvite("abc12345")).thenReturn(Optional.of(grupoCom(usuario(1L))));
        when(userService.buscarPorId(2L)).thenReturn(usuario(2L));
        when(friendGroupRepository.save(any(FriendGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendGroupResponse resultado = friendGroupService.entrarPorCodigo(" ABC12345 ", 2L);

        assertThat(resultado.membros()).extracting(UsuarioResumo::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Entrar duas vezes nao duplica o membro")
    void entrarDuasVezesEInofensivo() {
        when(friendGroupRepository.findByCodigoConvite("abc12345"))
            .thenReturn(Optional.of(grupoCom(usuario(1L), usuario(2L))));

        FriendGroupResponse resultado = friendGroupService.entrarPorCodigo("abc12345", 2L);

        assertThat(resultado.membros()).hasSize(2);
        verify(friendGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Codigo inexistente vira 404")
    void codigoInvalido() {
        when(friendGroupRepository.findByCodigoConvite("zzzzzzzz")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendGroupService.entrarPorCodigo("zzzzzzzz", 2L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Quem nao e membro nao enxerga o grupo")
    void naoMembroNaoAcessa() {
        when(friendGroupRepository.findById(10L)).thenReturn(Optional.of(grupoCom(usuario(1L))));

        assertThatThrownBy(() -> friendGroupService.buscarGrupoDoMembro(10L, 99L))
            .isInstanceOf(AcessoNegadoException.class);
    }
}
