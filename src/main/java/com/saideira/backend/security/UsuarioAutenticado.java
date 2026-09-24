package com.saideira.backend.security;

import com.saideira.backend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta a entidade User para o contrato do Spring Security, carregando o id junto.
 * Assim os controllers sabem quem esta logado sem receber 'usuarioId' da requisicao.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String email;
    private final String senhaHash;

    public UsuarioAutenticado(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.senhaHash = user.getSenhaHash();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
