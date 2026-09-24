package com.saideira.backend.service;

import com.saideira.backend.dto.AuthResponse;
import com.saideira.backend.dto.UsuarioResponse;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.UserRepository;
import com.saideira.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Cadastro e login. E o unico lugar que emite token. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        UserService userService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse cadastrar(String email, String senha, String nome) {
        return montarResposta(userService.cadastrar(email, senha, nome));
    }

    public AuthResponse login(String email, String senha) {
        User usuario = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new BadCredentialsException("credenciais invalidas"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new BadCredentialsException("credenciais invalidas");
        }
        return montarResposta(usuario);
    }

    private AuthResponse montarResposta(User usuario) {
        String token = jwtService.gerarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(token, jwtService.getValidadeMs(), UsuarioResponse.de(usuario));
    }
}
