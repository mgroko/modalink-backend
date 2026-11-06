package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;

class OrigenInicioResolverTest {

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 5, 12, 0);

    @Test
    void resolver_actividadNula_devuelveNull() {
        assertThat(OrigenInicioResolver.resolver(
                null, EstadoProyecto.CONFIRMADO, true)).isNull();
    }

    @Test
    void resolver_cancelada_devuelveNull() {
        Actividad actividad = actividad(EstadoActividad.CANCELADA, INICIO);

        assertThat(OrigenInicioResolver.resolver(
                actividad, EstadoProyecto.CONFIRMADO, true)).isNull();
    }

    @Test
    void resolver_enCursoYFinalizada_devuelveReal() {
        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.EN_CURSO, INICIO),
                EstadoProyecto.PUBLICADO,
                false))
                .isEqualTo(OrigenInicio.REAL);

        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.FINALIZADA, null),
                EstadoProyecto.CONFIRMADO,
                false))
                .isEqualTo(OrigenInicio.REAL);
    }

    @Test
    void resolver_pendienteSinInicio_devuelveProyectada() {
        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, null),
                EstadoProyecto.CONFIRMADO,
                true))
                .isEqualTo(OrigenInicio.PROYECTADA);
    }

    @Test
    void resolver_pendienteConInicioYAsignacionesEnConfirmado_devuelveConfirmada() {
        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, INICIO),
                EstadoProyecto.CONFIRMADO,
                true))
                .isEqualTo(OrigenInicio.CONFIRMADA);
    }

    @Test
    void resolver_pendienteConInicioSinAsignaciones_devuelveProyectada() {
        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, INICIO),
                EstadoProyecto.CONFIRMADO,
                false))
                .isEqualTo(OrigenInicio.PROYECTADA);
    }

    @Test
    void resolver_pendienteEnProyectoNoConfirmado_devuelveProyectada() {
        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, INICIO),
                EstadoProyecto.PUBLICADO,
                true))
                .isEqualTo(OrigenInicio.PROYECTADA);

        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, INICIO),
                EstadoProyecto.BORRADOR,
                true))
                .isEqualTo(OrigenInicio.PROYECTADA);
    }

    @Test
    void resolver_conProyecto_usaSuEstado() {
        Proyecto proyecto = Proyecto.builder()
                .estado(EstadoProyecto.CONFIRMADO)
                .build();

        assertThat(OrigenInicioResolver.resolver(
                actividad(EstadoActividad.PENDIENTE, INICIO),
                proyecto,
                true))
                .isEqualTo(OrigenInicio.CONFIRMADA);
    }

    @Test
    void esInmovible_soloEnCursoYFinalizada() {
        assertThat(OrigenInicioResolver.esInmovible(
                actividad(EstadoActividad.EN_CURSO, INICIO))).isTrue();
        assertThat(OrigenInicioResolver.esInmovible(
                actividad(EstadoActividad.FINALIZADA, INICIO))).isTrue();
        assertThat(OrigenInicioResolver.esInmovible(
                actividad(EstadoActividad.PENDIENTE, INICIO))).isFalse();
        assertThat(OrigenInicioResolver.esInmovible(
                actividad(EstadoActividad.CANCELADA, INICIO))).isFalse();
        assertThat(OrigenInicioResolver.esInmovible(null)).isFalse();
    }

    private Actividad actividad(EstadoActividad estado, LocalDateTime inicio) {
        return Actividad.builder()
                .idActividad(1L)
                .nombre("Actividad")
                .estado(estado)
                .duracionMinutos(60)
                .fechaHoraInicio(inicio)
                .build();
    }
}
