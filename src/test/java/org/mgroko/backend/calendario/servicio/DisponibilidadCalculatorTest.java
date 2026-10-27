package org.mgroko.backend.calendario.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;

class DisponibilidadCalculatorTest {

    private final DisponibilidadCalculator calculadora = new DisponibilidadCalculator();

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 26, 9, 0);

    @Test
    void hayHueco_ventanaLibreMayorQueLaDuracion_devuelveTrue() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(9));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(), 120)).isTrue();
    }

    @Test
    void hayHueco_bloqueoAlInicioDejaHuecoSuficiente_devuelveTrue() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(9));
        Intervalo bloqueo = new Intervalo(BASE, BASE.plusMinutes(30));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(bloqueo), 120)).isTrue();
    }

    @Test
    void hayHueco_bloqueoParteLaVentanaYNoAlcanza_devuelveFalse() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(1));
        Intervalo bloqueo = new Intervalo(BASE, BASE.plusMinutes(30));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(bloqueo), 45)).isFalse();
    }

    @Test
    void hayHueco_bloqueosSolapadosSeAcumulan_devuelveFalse() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(2));
        Intervalo primero = new Intervalo(BASE, BASE.plusMinutes(40));
        Intervalo segundo = new Intervalo(BASE.plusMinutes(20), BASE.plusMinutes(90));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(primero, segundo), 60)).isFalse();
    }

    @Test
    void hayHueco_bloqueoQueCubreTodaLaVentana_devuelveFalse() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(2));
        Intervalo bloqueo = new Intervalo(BASE.minusHours(1), BASE.plusHours(3));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(bloqueo), 30)).isFalse();
    }

    @Test
    void hayHueco_bloqueoEmpiezaAntesYTerminaDentro_devuelveElHuecoRestante() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(2));
        Intervalo bloqueo = new Intervalo(BASE.minusHours(1), BASE.plusMinutes(30));

        assertThat(calculadora.hayHueco(List.of(ventana), List.of(bloqueo), 90)).isTrue();
        assertThat(calculadora.hayHueco(List.of(ventana), List.of(bloqueo), 91)).isFalse();
    }

    @Test
    void hayHueco_variasVentanas_buscaEnTodas() {
        Intervalo manana = new Intervalo(BASE, BASE.plusHours(1));
        Intervalo tarde = new Intervalo(BASE.plusHours(5), BASE.plusHours(7));

        assertThat(calculadora.hayHueco(List.of(manana, tarde), List.of(), 120)).isTrue();
    }

    @Test
    void hayHueco_sinVentanas_devuelveFalse() {
        assertThat(calculadora.hayHueco(List.of(), List.of(), 30)).isFalse();
    }

    @Test
    void hayHueco_duracionCero_devuelveTrue() {
        assertThat(calculadora.hayHueco(List.of(), List.of(), 0)).isTrue();
    }

    @Test
    void huecosLibres_bloqueosSolapadosSeFusionan() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(4));
        List<Intervalo> huecos = calculadora.huecosLibres(
                List.of(ventana),
                List.of(
                        new Intervalo(BASE.plusHours(1), BASE.plusHours(2)),
                        new Intervalo(BASE.plusMinutes(90), BASE.plusHours(3))));

        assertThat(huecos).containsExactly(
                new Intervalo(BASE, BASE.plusHours(1)),
                new Intervalo(BASE.plusHours(3), BASE.plusHours(4)));
    }

    @Test
    void primerHueco_aplicaMargenSimetricoALaActividadNueva() {
        Intervalo ventana = new Intervalo(BASE, BASE.plusHours(4));

        assertThat(calculadora.primerHueco(
                List.of(ventana), List.of(), 60, 30))
                .contains(new Intervalo(BASE.plusMinutes(30), BASE.plusMinutes(90)));
    }

    @Test
    void intersecarHuecos_devuelveSoloElTramoComun() {
        List<Intervalo> primero = List.of(new Intervalo(BASE, BASE.plusHours(3)));
        List<Intervalo> segundo = List.of(new Intervalo(BASE.plusHours(1), BASE.plusHours(4)));

        assertThat(calculadora.intersecarHuecos(List.of(primero, segundo)))
                .containsExactly(new Intervalo(BASE.plusHours(1), BASE.plusHours(3)));
    }

    @Test
    void primerHuecoEnHuecos_sinInterseccionEsVacio() {
        assertThat(calculadora.primerHuecoEnHuecos(
                List.of(new Intervalo(BASE, BASE.plusMinutes(30))), 60, 0))
                .isEmpty();
    }
}
