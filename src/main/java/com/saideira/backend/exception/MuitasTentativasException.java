package com.saideira.backend.exception;

/** Login bloqueado por excesso de senhas erradas (vira 429). */
public class MuitasTentativasException extends RuntimeException {

    private final long minutos;

    public MuitasTentativasException(long minutos) {
        super("Muitas tentativas de login. Espere " + minutos + (minutos == 1 ? " minuto" : " minutos")
            + " e tente de novo — ou peça para o admin gerar uma senha nova.");
        this.minutos = minutos;
    }

    public long getMinutos() {
        return minutos;
    }
}
