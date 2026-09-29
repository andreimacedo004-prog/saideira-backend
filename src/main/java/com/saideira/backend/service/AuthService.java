package com.saideira.backend.service;

import com.saideira.backend.dto.AuthResponse;
import com.saideira.backend.dto.UsuarioResponse;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.UserRepository;
import com.saideira.backend.security.Administradores;
import com.saideira.backend.security.JwtService;
import com.saideira.backend.security.LimiteDeTentativas;
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
    private final Administradores administradores;
    private final LimiteDeTentativas limiteDeTentativas;
    private final String hashFalso;

    public AuthService(
        UserRepository userRepository,
        UserService userService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        Administradores administradores,
        LimiteDeTentativas limiteDeTentativas
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.administradores = administradores;
        this.limiteDeTentativas = limiteDeTentativas;
        // Conta que nao existe tambem paga o custo do BCrypt: a resposta nao
        // denuncia pelo tempo se o e-mail tem conta ou nao
        this.hashFalso = passwordEncoder.encode("saideira-conta-inexistente");
    }

    public AuthResponse cadastrar(String email, String senha, String nome) {
        return montarResposta(userService.cadastrar(email, senha, nome));
    }

    /**
     * @param ip de onde veio a tentativa (para o freio de chute de senha)
     */
    public AuthResponse login(String email, String senha, String ip) {
        String emailNormalizado = email.trim().toLowerCase();
        limiteDeTentativas.verificar(emailNormalizado, ip);

        User usuario = userRepository.findByEmail(emailNormalizado).orElse(null);
        boolean senhaConfere = passwordEncoder.matches(senha, usuario != null ? usuario.getSenhaHash() : hashFalso);
        if (usuario == null || !senhaConfere) {
            limiteDeTentativas.registrarFalha(emailNormalizado, ip);
            throw new BadCredentialsException("credenciais invalidas");
        }
        limiteDeTentativas.registrarSucesso(emailNormalizado);
        return montarResposta(usuario);
    }

    private AuthResponse montarResposta(User usuario) {
        String token = jwtService.gerarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(
            token, jwtService.getValidadeMs(), UsuarioResponse.de(usuario, administradores.eAdmin(usuario.getEmail()))
        );
    }
}
