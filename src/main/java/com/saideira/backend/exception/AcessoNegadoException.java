package com.saideira.backend.exception;

/** Vira HTTP 403. Ex: ver o feed de um grupo do qual voce nao faz parte. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
