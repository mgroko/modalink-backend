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
    void recalcular_cadenaPendiente_programaHaciaAtrasDesdeLaEntrega() {
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10), null);

        Actividad b = actividad(2L, "B", 120, EstadoActividad.PENDIENTE, null);
        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        b.setPredecesoras(Set.of(a));

        cronogramaService.recalcular(planificacion, List.of(a, b));

        assertThat(b.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 10, 21, 59));
        assertThat(a.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 10, 20, 59));
    }

    @Test
    void recalcular_sucesoraNoPendiente_usaSuInicioFijo() {
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10), null);

        Actividad enCurso = actividad(
                2L,
                "En curso",
                120,
                EstadoActividad.EN_CURSO,
                LocalDateTime.of(2026, 11, 5, 10, 0));
        LocalDateTime inicioOriginal = enCurso.getFechaHoraInicio();

        Actividad pendiente = actividad(1L, "Pendiente", 60, EstadoActividad.PENDIENTE, null);
        enCurso.setPredecesoras(Set.of(pendiente));

        cronogramaService.recalcular(planificacion, List.of(pendiente, enCurso));

        assertThat(pendiente.getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 11, 5, 9, 0));
        assertThat(enCurso.getFechaHoraInicio()).isEqualTo(inicioOriginal);
    }

    @Test
    void recalcular_inicioAnteriorAlProyecto_lanzaExcepcion() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 10),
                LocalDateTime.of(2026, 11, 10, 21, 0));

        Actividad actividad = actividad(1L, "A", 240, EstadoActividad.PENDIENTE, null);

        assertThatThrownBy(() -> cronogramaService.recalcular(planificacion, List.of(actividad)))
                .isInstanceOf(CronogramaInviableException.class);
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
