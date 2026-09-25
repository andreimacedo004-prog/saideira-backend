package com.saideira.backend.service;

import com.saideira.backend.dto.RetrospectivaResponse;
import com.saideira.backend.model.Beer;
import com.saideira.backend.model.CervejaDoRole;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FormatoCerveja;
import com.saideira.backend.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrospectivaServiceTest {

    private User usuario(long id) {
        User u = new User();
        u.setId(id);
        u.setNome("Usuario " + id);
        return u;
    }

    private Beer cerveja(long id, String nome) {
        Beer b = new Beer();
        b.setId(id);
        b.setNome(nome);
        return b;
    }

    private CheckIn checkIn(User autor, CervejaDoRole... cervejas) {
        CheckIn c = new CheckIn();
        c.setAutor(autor);
        c.getCervejas().addAll(List.of(cervejas));
        return c;
    }

    @Test
    @DisplayName("Soma litros pelo formato x quantidade: meu total e o total da galera")
    void somaLitros() {
        User ana = usuario(1);
        User bia = usuario(2);
        Beer heineken = cerveja(10, "Heineken");
        Beer brahma = cerveja(11, "Brahma Chopp");

        List<CheckIn> checkIns = List.of(
            // Ana: 3 latas de Heineken (1050 ml) + 2 garrafas de Brahma (1200 ml)
            checkIn(ana, new CervejaDoRole(heineken, FormatoCerveja.LATA, 3), new CervejaDoRole(brahma, FormatoCerveja.GARRAFA, 2)),
            // Ana: 1 litrao de Brahma (1000 ml)
            checkIn(ana, new CervejaDoRole(brahma, FormatoCerveja.LITRAO, 1)),
            // Bia: 4 chopps de Heineken (1200 ml)
            checkIn(bia, new CervejaDoRole(heineken, FormatoCerveja.CHOPP, 4))
        );

        RetrospectivaResponse r = RetrospectivaService.calcular(5L, checkIns, ana.getId());

        assertThat(r.eu().roles()).isEqualTo(2);
        assertThat(r.eu().unidades()).isEqualTo(6);
        assertThat(r.eu().litros()).isEqualTo(3.3);           // 3250 ml
        assertThat(r.eu().cervejasDiferentes()).isEqualTo(2);

        assertThat(r.grupo().roles()).isEqualTo(3);
        assertThat(r.grupo().litros()).isEqualTo(4.5);        // 4450 ml
        assertThat(r.grupo().cervejasDiferentes()).isEqualTo(2);

        // Minhas cervejas, da mais tomada para a menos (em volume)
        assertThat(r.minhasCervejas()).extracting(c -> c.cerveja().nome()).containsExactly("Brahma Chopp", "Heineken");
        assertThat(r.minhasCervejas().get(0).litros()).isEqualTo(2.2);
        assertThat(r.minhasCervejas().get(0).unidades()).isEqualTo(3);
    }

    @Test
    @DisplayName("Quem nao registrou nada ve zero, sem estourar")
    void semNada() {
        RetrospectivaResponse r = RetrospectivaService.calcular(5L, List.of(), 1L);

        assertThat(r.eu().litros()).isZero();
        assertThat(r.grupo().roles()).isZero();
        assertThat(r.minhasCervejas()).isEmpty();
    }
}
