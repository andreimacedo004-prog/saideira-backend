package com.saideira.backend.model;

/**
 * Em que veio a cerveja. O volume de cada formato e o que permite somar
 * litros na retrospectiva sem ninguem precisar digitar mililitro.
 */
public enum FormatoCerveja {
    LATA("Lata", 350),
    LATAO("Latão", 473),
    LONG_NECK("Long neck", 330),
    GARRAFA("Garrafa", 600),
    LITRAO("Litrão", 1000),
    CHOPP("Chopp", 300);

    private final String rotulo;
    private final int mililitros;

    FormatoCerveja(String rotulo, int mililitros) {
        this.rotulo = rotulo;
        this.mililitros = mililitros;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getMililitros() {
        return mililitros;
    }
}
