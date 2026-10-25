package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;

class CronogramaServiceTest {

    private final CronogramaService cronogramaService = new CronogramaService();

    @Test
    void recalcular_sinFechaEntrega_lanzaExcepcion() {
        Planificacion planificacion = Planificacion.builder()
                .proyecto(Proyecto.builder().build())
                .build();

        assertThatThrownBy(() -> cronogramaService.recalcular(planificacion, List.of()))
                .isInstanceOf(FechaEntregaNoDefinidaException.class);
    }

    @Test
    void recalcular_cadenaPendiente_programaHaciaAdelanteDesdeInicioProyecto() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 10),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad b = actividad(2L, "B", 120, EstadoActividad.PENDIENTE, null);
        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        b.setPredecesoras(Set.of(a));

        EstadoCronograma estado = cronogramaService.recalcular(
                planificacion, List.of(a, b));

        assertThat(estado).isEqualTo(EstadoCronograma.NORMAL);
        assertThat(a.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
        assertThat(b.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
    }

    @Test
    void recalcular_sucesoraPendienteDeNoPendiente_continuaDesdeSuFinFijo() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 10),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad enCurso = actividad(
                1L,
                "En curso",
                120,
                EstadoActividad.EN_CURSO,
                LocalDateTime.of(2026, 11, 5, 10, 0));
        LocalDateTime inicioOriginal = enCurso.getFechaHoraInicio();

        Actividad pendiente = actividad(2L, "Pendiente", 60, EstadoActividad.PENDIENTE, null);
        pendiente.setPredecesoras(Set.of(enCurso));

        cronogramaService.recalcular(planificacion, List.of(enCurso, pendiente));

        assertThat(enCurso.getFechaHoraInicio()).isEqualTo(inicioOriginal);
        assertThat(pendiente.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 5, 12, 0));
    }

    @Test
    void recalcular_superaFechaEntrega_lanzaExcepcion() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 2),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad actividad = actividad(1L, "A", 2880, EstadoActividad.PENDIENTE, null);

        assertThatThrownBy(() -> cronogramaService.recalcular(planificacion, List.of(actividad)))
                .isInstanceOf(CronogramaInviableException.class)
                .hasMessageContaining("supera la fecha de entrega");
    }

    @Test
    void recalcularTolerandoDesborde_superaEntrega_marcaAlertaYConservaInicioTemprano() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 2),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad actividad = actividad(1L, "A", 2880, EstadoActividad.PENDIENTE, null);

        EstadoCronograma estado = cronogramaService.recalcularTolerandoDesborde(
                planificacion, List.of(actividad));

        assertThat(estado).isEqualTo(EstadoCronograma.ALERTA_DESBORDE);
        assertThat(actividad.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    @Test
    void evaluar_conFinPosteriorALaEntrega_marcaAlerta() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 2),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad actividad = actividad(
                1L,
                "A",
                120,
                EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 2, 23, 0));

        assertThat(cronogramaService.evaluar(planificacion, List.of(actividad)))
                .isEqualTo(EstadoCronograma.ALERTA_DESBORDE);
    }

    @Test
    void evaluar_sinFechaEntrega_marcaNormal() {
        Planificacion planificacion = Planificacion.builder()
                .proyecto(Proyecto.builder().build())
                .build();

        assertThat(cronogramaService.evaluar(planificacion, List.of()))
                .isEqualTo(EstadoCronograma.NORMAL);
    }

    private Planificacion planificacion(LocalDate fechaEntrega, LocalDateTime fechaInicioProyecto) {
        Proyecto proyecto = Proyecto.builder()
                .fechaInicio(fechaInicioProyecto)
                .build();
        return Planificacion.builder()
                .fechaEntrega(fechaEntrega)
                .proyecto(proyecto)
                .build();
    }

    private Actividad actividad(
            Long id,
            String nombre,
            int duracionMinutos,
            EstadoActividad estado,
            LocalDateTime fechaHoraInicio) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .duracionMinutos(duracionMinutos)
                .estado(estado)
                .fechaHoraInicio(fechaHoraInicio)
                .build();
    }
}
