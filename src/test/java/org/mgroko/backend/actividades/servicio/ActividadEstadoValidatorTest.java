package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;

class ActividadEstadoValidatorTest {

    @Test
    void pendienteAEnCurso_conPredecesoraPendiente_lanzaExcepcion() {
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE);
        Actividad sucesora = actividad(
                2L, "Sucesora", EstadoActividad.PENDIENTE);
        sucesora.setPredecesoras(Set.of(predecesora));

        assertThatThrownBy(() -> ActividadEstadoValidator.validar(
                sucesora, EstadoActividad.EN_CURSO))
                .isInstanceOf(TransicionEstadoActividadInvalidaException.class)
                .hasMessageContaining("Primero debe finalizar la predecesora");
    }

    @Test
    void pendienteAEnCurso_conPredecesoraFinalizada_permite() {
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.FINALIZADA);
        Actividad sucesora = actividad(
                2L, "Sucesora", EstadoActividad.PENDIENTE);
        sucesora.setPredecesoras(Set.of(predecesora));

        assertThatCode(() -> ActividadEstadoValidator.validar(
                sucesora, EstadoActividad.EN_CURSO))
                .doesNotThrowAnyException();
    }

    @Test
    void pendienteAEnCurso_conPredecesoraCancelada_permite() {
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.CANCELADA);
        Actividad sucesora = actividad(
                2L, "Sucesora", EstadoActividad.PENDIENTE);
        sucesora.setPredecesoras(Set.of(predecesora));

        assertThatCode(() -> ActividadEstadoValidator.validar(
                sucesora, EstadoActividad.EN_CURSO))
                .doesNotThrowAnyException();
    }

    @Test
    void pendienteACancelada_conPredecesoraPendiente_permite() {
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE);
        Actividad sucesora = actividad(
                2L, "Sucesora", EstadoActividad.PENDIENTE);
        sucesora.setPredecesoras(Set.of(predecesora));

        assertThatCode(() -> ActividadEstadoValidator.validar(
                sucesora, EstadoActividad.CANCELADA))
                .doesNotThrowAnyException();
    }

    private Actividad actividad(Long id, String nombre, EstadoActividad estado) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .estado(estado)
                .duracionMinutos(60)
                .build();
    }
}
