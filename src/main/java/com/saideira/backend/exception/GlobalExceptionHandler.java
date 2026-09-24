package com.saideira.backend.exception;

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

    // Regra de negocio (ex: "intervalo minimo entre check-ins")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleRegraDeNegocio(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
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
            .body(Map.of("erro", "E-mail ou senha invalidos"));
    }

    // Validacao dos DTOs (@NotBlank, @Size, @Pattern...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                e -> e.getField(),
                e -> e.getDefaultMessage() == null ? "valor invalido" : e.getDefaultMessage(),
                (msg1, msg2) -> msg1
            ));
        return ResponseEntity.badRequest().body(erros);
    }

    // JSON malformado ou enum desconhecido no corpo (ex: "tipo": "BALADA")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Corpo da requisicao invalido"));
    }

    // Parametro de URL com tipo errado (ex: /reacoes/XPTO ou /checkins/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Valor invalido para '" + ex.getName() + "'"));
    }

    // Duas requisicoes iguais ao mesmo tempo batendo numa constraint unica do banco
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflito(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("erro", "Esse registro ja existe ou entra em conflito com outro"));
    }
}
