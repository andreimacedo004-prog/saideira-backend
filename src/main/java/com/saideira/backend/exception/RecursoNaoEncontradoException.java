package com.saideira.backend.exception;

/** Vira HTTP 404. Ex: buscar um check-in que nao existe. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
