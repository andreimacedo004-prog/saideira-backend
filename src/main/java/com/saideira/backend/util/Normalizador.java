package com.saideira.backend.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deixa textos digitados a mao comparaveis entre si.
 *
 * "Bar do Zé", "bar do ze" e "  BAR  DO   ZÉ " viram todos "bar do ze".
 * E isso que faz o bonus de lugar novo e o catalogo de cervejas nao
 * tratarem como coisas diferentes o que so foi digitado diferente.
 */
public final class Normalizador {

    private Normalizador() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return semAcento
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("\\s+", " ");
    }
}
