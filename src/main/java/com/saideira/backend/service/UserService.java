package com.saideira.backend.service;

import com.saideira.backend.dto.UsuarioResponse;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.UserRepository;
import com.saideira.backend.security.Administradores;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, busca e atualizacao de perfil. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Administradores administradores;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Administradores administradores) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.administradores = administradores;
    }

    @Transactional
    public User cadastrar(String email, String senha, String nome) {
        String emailNormalizado = email.trim().toLowerCase();
        if (senha.length() < 8) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres");
        }
        if (userRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail");
        }
        User novo = new User();
        novo.setEmail(emailNormalizado);
        novo.setSenhaHash(passwordEncoder.encode(senha));
        novo.setNome(nome.trim());
        return userRepository.save(novo);
    }

    @Transactional(readOnly = true)
    public User buscarPorId(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil(Long id) {
        User usuario = buscarPorId(id);
        return UsuarioResponse.de(usuario, administradores.eAdmin(usuario.getEmail()));
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(Long id, String nome, String bio, String fotoUrl) {
        User usuario = buscarPorId(id);
        usuario.setNome(nome.trim());
        usuario.setBio(vazioViraNulo(bio));
        usuario.setFotoUrl(vazioViraNulo(fotoUrl));
        User salvo = userRepository.save(usuario);
        return UsuarioResponse.de(salvo, administradores.eAdmin(salvo.getEmail()));
    }

    private static String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
