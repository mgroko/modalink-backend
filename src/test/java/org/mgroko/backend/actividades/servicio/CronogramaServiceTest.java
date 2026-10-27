package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.dto.ProyeccionCpm;
import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;

class CronogramaServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-25T12:00:00Z"),
            ZoneId.of("UTC"));

    private final CronogramaService cronogramaService = new CronogramaService(RELOJ);

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
        assertThat(fechaProyectada(planificacion, List.of(a, b), 1L))
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
        assertThat(fechaProyectada(planificacion, List.of(a, b), 2L))
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
        assertThat(fechaProyectada(planificacion, List.of(enCurso, pendiente), 2L))
                .isEqualTo(LocalDateTime.of(2026, 11, 5, 12, 0));
    }

    @Test
    void recalcular_sinFechaInicio_programaDesdeElInicioDelDiaActual() {
        Planificacion planificacion = Planificacion.builder()
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .proyecto(Proyecto.builder().build())
                .build();

        Actividad actividad = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);

        cronogramaService.recalcular(planificacion, List.of(actividad));

        assertThat(fechaProyectada(planificacion, List.of(actividad), 1L))
                .isEqualTo(LocalDateTime.of(2026, 10, 25, 12, 0));
    }

    @Test
    void recalcular_fechaInicioPasada_replanificaDesdeHoy() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 12, 1),
                LocalDateTime.of(2026, 10, 1, 9, 0));

        Actividad actividad = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);

        cronogramaService.recalcular(planificacion, List.of(actividad));

        assertThat(fechaProyectada(planificacion, List.of(actividad), 1L))
                .isEqualTo(LocalDateTime.of(2026, 10, 25, 12, 0));
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
        assertThat(fechaProyectada(planificacion, List.of(actividad), 1L))
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    @Test
    void evaluar_cronogramaNoEntraEnLaEntrega_marcaAlerta() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 1),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad actividad = actividad(1L, "A", 2880, EstadoActividad.PENDIENTE, null);

        assertThat(cronogramaService.evaluar(planificacion, List.of(actividad)))
                .isEqualTo(EstadoCronograma.ALERTA_DESBORDE);
    }

    @Test
    void evaluar_cronogramaEntraEnLaEntrega_marcaNormal() {
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 11, 10),
                LocalDateTime.of(2026, 11, 1, 9, 0));

        Actividad actividad = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);

        assertThat(cronogramaService.evaluar(planificacion, List.of(actividad)))
                .isEqualTo(EstadoCronograma.NORMAL);
    }

    @Test
    void evaluar_sinFechaEntrega_marcaNormal() {
        Planificacion planificacion = Planificacion.builder()
                .proyecto(Proyecto.builder().build())
                .build();

        assertThat(cronogramaService.evaluar(planificacion, List.of()))
                .isEqualTo(EstadoCronograma.SIN_FECHA_ENTREGA);
    }

    @Test
    void calcular_noModificaLasActividades() {
        LocalDateTime fechaPersistida = LocalDateTime.of(2026, 10, 1, 9, 0);
        Actividad actividad = actividad(
                1L, "A", 60, EstadoActividad.PENDIENTE, fechaPersistida);

        CalculoCpm calculo = cronogramaService.calcular(List.of(actividad));

        assertThat(actividad.getFechaHoraInicio()).isEqualTo(fechaPersistida);
        assertThat(calculo.nodoDe(1L).inicioTemprano()).isZero();
    }

    @Test
    void calcular_cadenaConParalelas_calculaHolgurasYCriticas() {
        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        Actividad b = actividad(2L, "B", 120, EstadoActividad.PENDIENTE, null);
        Actividad c = actividad(3L, "C", 60, EstadoActividad.PENDIENTE, null);
        c.setPredecesoras(Set.of(a, b));

        CalculoCpm calculo = cronogramaService.calcular(List.of(a, b, c));

        assertThat(calculo.makespan()).isEqualTo(180);

        NodoCpm nodoA = calculo.nodoDe(1L);
        NodoCpm nodoB = calculo.nodoDe(2L);
        NodoCpm nodoC = calculo.nodoDe(3L);

        assertThat(nodoA.inicioTemprano()).isZero();
        assertThat(nodoA.finTemprano()).isEqualTo(60);
        assertThat(nodoA.inicioTardioMakespan()).isEqualTo(60);
        assertThat(nodoA.finTardioMakespan()).isEqualTo(120);
        assertThat(nodoA.holguraMakespan()).isEqualTo(60);
        assertThat(nodoA.critica()).isFalse();
        assertThat(nodoA.ventanaInicio().inicioMinutos()).isZero();
        assertThat(nodoA.ventanaInicio().finMinutos()).isEqualTo(60);

        assertThat(nodoB.holguraMakespan()).isZero();
        assertThat(nodoB.critica()).isTrue();
        assertThat(nodoC.inicioTemprano()).isEqualTo(120);
        assertThat(nodoC.finTemprano()).isEqualTo(180);
        assertThat(nodoC.holguraMakespan()).isZero();
        assertThat(nodoC.critica()).isTrue();
    }

    @Test
    void calcular_ciclo_lanzaExcepcion() {
        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        Actividad b = actividad(2L, "B", 60, EstadoActividad.PENDIENTE, null);
        a.setPredecesoras(Set.of(b));
        b.setPredecesoras(Set.of(a));

        assertThatThrownBy(() -> cronogramaService.calcular(List.of(a, b)))
                .isInstanceOf(CronogramaInviableException.class)
                .hasMessageContaining("ciclo");
    }

    @Test
    void calcular_esIndependienteDelReloj() {
        CronogramaService otroReloj = new CronogramaService(Clock.fixed(
                Instant.parse("2030-06-01T12:00:00Z"), ZoneId.of("UTC")));

        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        Actividad b = actividad(2L, "B", 120, EstadoActividad.PENDIENTE, null);
        b.setPredecesoras(Set.of(a));

        CalculoCpm esperado = cronogramaService.calcular(List.of(a, b));
        CalculoCpm obtenido = otroReloj.calcular(List.of(a, b));

        assertThat(obtenido).isEqualTo(esperado);
    }

    @Test
    void proyectar_desplazaOffsetsYMarcaTentativa() {
        Actividad a = actividad(1L, "A", 60, EstadoActividad.PENDIENTE, null);
        Actividad b = actividad(2L, "B", 120, EstadoActividad.PENDIENTE, null);
        b.setPredecesoras(Set.of(a));

        CalculoCpm calculo = cronogramaService.calcular(List.of(a, b));
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 9, 0);

        ProyeccionCpm proyeccion = cronogramaService.proyectar(calculo, inicio, true);

        assertThat(proyeccion.inicio()).isEqualTo(inicio);
        assertThat(proyeccion.tentativa()).isTrue();
        assertThat(proyeccion.fechaDe(1L)).isEqualTo(inicio);
        assertThat(proyeccion.fechaDe(2L)).isEqualTo(inicio.plusMinutes(60));
        assertThat(proyeccion.fechas()).containsOnlyKeys(1L, 2L);
    }

    @Test
    void inicioEfectivo_sinFechaInicio_devuelveInicioDelDia() {
        Planificacion planificacion = Planificacion.builder()
                .proyecto(Proyecto.builder().build())
                .build();

        assertThat(cronogramaService.inicioEfectivo(planificacion))
                .isEqualTo(LocalDateTime.of(2026, 10, 25, 0, 0));
        assertThat(cronogramaService.esTentativa(planificacion)).isTrue();
    }

    @Test
    void inicioEfectivo_conFechaInicioFutura_devuelveFechaDelProyecto() {
        LocalDateTime fechaInicio = LocalDateTime.of(2026, 11, 1, 9, 0);
        Planificacion planificacion = planificacion(
                LocalDate.of(2026, 12, 1), fechaInicio);

        assertThat(cronogramaService.inicioEfectivo(planificacion)).isEqualTo(fechaInicio);
        assertThat(cronogramaService.esTentativa(planificacion)).isFalse();
    }

    private LocalDateTime fechaProyectada(
            Planificacion planificacion,
            List<Actividad> actividades,
            Long idActividad) {
        CalculoCpm calculo = cronogramaService.calcularProyeccion(planificacion, actividades);
        return cronogramaService.proyectar(
                calculo,
                cronogramaService.inicioEfectivo(planificacion),
                cronogramaService.esTentativa(planificacion))
                .fechaDe(idActividad);
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
