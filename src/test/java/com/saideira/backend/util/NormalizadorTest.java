package com.saideira.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorTest {

    @Test
    @DisplayName("Mesmo lugar digitado de jeitos diferentes vira o mesmo texto")
    void normaliza() {
        assertThat(Normalizador.normalizar("Bar do Zé")).isEqualTo("bar do ze");
        assertThat(Normalizador.normalizar("  BAR   DO ZÉ ")).isEqualTo("bar do ze");
        assertThat(Normalizador.normalizar("Açaí & Chopp")).isEqualTo("acai & chopp");
    }

    @Test
    @DisplayName("Nulo vira texto vazio, sem estourar")
    void nulo() {
        assertThat(Normalizador.normalizar(null)).isEmpty();
    }
}
