package com.saideira.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centraliza o tratamento de erros de todos os controllers.
 * Padrao da API: { "erro": "mensagem" } — exceto validacao de campos,
 * que devolve { "campo": "mensagem" } para o front marcar o campo certo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Regra de negocio (ex: "intervalo minimo entre check-ins").
    // Vai para o log (sem corpo da requisicao nem dados pessoais) para dar
    // para ver no Railway o que anda travando a galera.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleRegraDeNegocio(IllegalArgumentException ex, HttpServletRequest req) {
        log.info("Recusado {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, String>> handleMuitasTentativas(MuitasTentativasException ex, HttpServletRequest req) {
        log.warn("Login bloqueado por excesso de tentativas ({} min)", ex.getMinutos());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getMinutos() * 60))
            .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleEstadoInvalido(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, String>> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", ex.getMessage()));
    }

    // Login com e-mail ou senha errados — nunca dizemos qual dos dois errou
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleCredenciais(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("erro", "E-mail ou senha inválidos"));
    }

    // Validacao dos DTOs (@NotBlank, @Size, @Pattern...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // So o nome dos campos vai para o log, nunca o valor digitado
        log.info("Recusado {} {}: campos invalidos {}", req.getMethod(), req.getRequestURI(),
            ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()).distinct().toList());
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                e -> e.getField(),
                e -> e.getDefaultMessage() == null ? "valor inválido" : e.getDefaultMessage(),
                (msg1, msg2) -> msg1
            ));
        return ResponseEntity.badRequest().body(erros);
    }

    // JSON malformado ou enum desconhecido no corpo (ex: "tipo": "BALADA")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisição inválido"));
    }

    // Parametro de URL com tipo errado (ex: /reacoes/XPTO ou /checkins/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Valor inválido para '" + ex.getName() + "'"));
    }

    // Duas requisicoes iguais ao mesmo tempo batendo numa constraint unica do banco
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflito(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("erro", "Esse registro já existe ou entra em conflito com outro"));
    }
}
