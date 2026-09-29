package com.saideira.backend.security;

import com.saideira.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Ponte entre o UserRepository e o Spring Security. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Administradores administradores;

    public UsuarioDetailsService(UserRepository userRepository, Administradores administradores) {
        this.userRepository = userRepository;
        this.administradores = administradores;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .map(u -> new UsuarioAutenticado(u, administradores.eAdmin(u.getEmail())))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }

    /** Versao que devolve null em vez de lancar excecao — usada dentro do filtro JWT. */
    public UsuarioAutenticado carregarPorEmail(String email) {
        return userRepository.findByEmail(email)
            .map(u -> new UsuarioAutenticado(u, administradores.eAdmin(u.getEmail())))
            .orElse(null);
    }
}
