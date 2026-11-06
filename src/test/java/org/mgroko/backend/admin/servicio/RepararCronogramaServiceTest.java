package org.mgroko.backend.admin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.RepararCronogramaRequest;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepararCronogramaServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;

    private RepararCronogramaService service;

    @BeforeEach
    void setUp() {
        service = new RepararCronogramaService(
                proyectoRepository,
                planificacionRepository,
                actividadRepository,
                asignacionActividadRepository,
                reprogramarProyectoService);
    }

    @Test
    void detectar_actividadPendienteAntesDeSuPredecesora_devuelveInconsistencia() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 0));
        Actividad objetivo = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 30));
        objetivo.setPredecesoras(Set.of(predecesora));

        configurarProyecto(proyecto, planificacion, predecesora, objetivo);

        var inconsistencias = service.detectar(null, null, null);

        assertThat(inconsistencias).hasSize(1);
        assertThat(inconsistencias.get(0).idActividad()).isEqualTo(2L);
        assertThat(inconsistencias.get(0).tipo())
                .isEqualTo(RepararCronogramaService.TIPO_INICIO_ANTES_DE_PREDECESORA);
    }

    @Test
    void detectar_proyectoPublicado_devuelveInconsistencia() {
        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 0));
        Actividad objetivo = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 30));
        objetivo.setPredecesoras(Set.of(predecesora));

        configurarProyecto(proyecto, planificacion, predecesora, objetivo);

        var inconsistencias = service.detectar(null, null, null);

        assertThat(inconsistencias).hasSize(1);
        assertThat(inconsistencias.get(0).tipo())
                .isEqualTo(RepararCronogramaService.TIPO_INICIO_ANTES_DE_PREDECESORA);
    }

    @Test
    void detectar_actividadEjecutadaSinInicio_devuelveInconsistencia() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad actividad = actividad(
                2L, "En curso", EstadoActividad.EN_CURSO, null);

        configurarProyecto(proyecto, planificacion, actividad);

        var inconsistencias = service.detectar(null, null, null);

        assertThat(inconsistencias).hasSize(1);
        assertThat(inconsistencias.get(0).tipo())
                .isEqualTo(RepararCronogramaService.TIPO_ACTIVIDAD_EJECUTADA_SIN_INICIO);
    }

    @Test
    void detectar_pendienteAntesDelInicioDelProyecto_devuelveInconsistencia() {
        Proyecto proyecto = proyecto(
                EstadoProyecto.CONFIRMADO,
                LocalDateTime.of(2026, 11, 10, 0, 0));
        Planificacion planificacion = planificacion(proyecto);
        Actividad actividad = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 5, 9, 0));

        configurarProyecto(proyecto, planificacion, actividad);

        var inconsistencias = service.detectar(null, null, null);

        assertThat(inconsistencias).hasSize(1);
        assertThat(inconsistencias.get(0).tipo())
                .isEqualTo(RepararCronogramaService.TIPO_INICIO_ANTES_DE_PROYECTO);
    }

    @Test
    void detectar_pendienteSinInicioConAsignacionesEnConfirmado_devuelveInconsistencia() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad actividad = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE, null);

        configurarProyecto(proyecto, planificacion, actividad);
        when(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(anyCollection()))
                .thenReturn(Set.of(2L));

        var inconsistencias = service.detectar(null, null, null);

        assertThat(inconsistencias).hasSize(1);
        assertThat(inconsistencias.get(0).tipo())
                .isEqualTo(RepararCronogramaService.TIPO_PENDIENTE_SIN_INICIO_CON_ASIGNACIONES);
    }

    @Test
    void reparar_dryRunNoPersiste() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 0));
        Actividad objetivo = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 30));
        objetivo.setPredecesoras(Set.of(predecesora));

        configurarProyecto(proyecto, planificacion, predecesora, objetivo);

        ReparacionCronogramaResponse response = service.reparar(
                new RepararCronogramaRequest(true, null, null, null));

        assertThat(response.dryRun()).isTrue();
        assertThat(response.totalDetectadas()).isEqualTo(1);
        assertThat(response.totalReparadas()).isZero();
        verify(reprogramarProyectoService, never())
                .reprogramarActividad(any(), any(), any(), any());
    }

    @Test
    void reparar_aplicaYReprogramaActividadPendiente() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad predecesora = actividad(
                1L, "Predecesora", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 0));
        Actividad objetivo = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE,
                LocalDateTime.of(2026, 11, 6, 9, 30));
        objetivo.setPredecesoras(Set.of(predecesora));

        configurarProyecto(proyecto, planificacion, predecesora, objetivo);

        ReparacionCronogramaResponse response = service.reparar(
                new RepararCronogramaRequest(false, null, null, null));

        assertThat(response.totalReparadas()).isEqualTo(1);
        verify(reprogramarProyectoService).reprogramarActividad(
                100L,
                2L,
                MotivoReprogramacion.REPARACION,
                Map.of());
    }

    @Test
    void reparar_actividadEjecutadaSinInicioNoSeRepara() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad actividad = actividad(
                2L, "En curso", EstadoActividad.EN_CURSO, null);

        configurarProyecto(proyecto, planificacion, actividad);

        ReparacionCronogramaResponse response = service.reparar(
                new RepararCronogramaRequest(false, null, null, null));

        assertThat(response.totalReparadas()).isZero();
        assertThat(response.totalFallidas()).isEqualTo(1);
        assertThat(response.errores()).hasSize(1);
        verify(reprogramarProyectoService, never())
                .reprogramarActividad(any(), any(), any(), any());
    }

    @Test
    void reparar_pendienteSinInicioConAsignacionesSeReprograma() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO, null);
        Planificacion planificacion = planificacion(proyecto);
        Actividad actividad = actividad(
                2L, "Objetivo", EstadoActividad.PENDIENTE, null);

        configurarProyecto(proyecto, planificacion, actividad);
        when(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(anyCollection()))
                .thenReturn(Set.of(2L));

        ReparacionCronogramaResponse response = service.reparar(
                new RepararCronogramaRequest(false, null, null, null));

        assertThat(response.totalReparadas()).isEqualTo(1);
        verify(reprogramarProyectoService).reprogramarActividad(
                100L,
                2L,
                MotivoReprogramacion.REPARACION,
                Map.of());
    }

    private void configurarProyecto(
            Proyecto proyecto,
            Planificacion planificacion,
            Actividad... actividades) {

        when(proyectoRepository.findByEstadoIn(
                List.of(EstadoProyecto.PUBLICADO, EstadoProyecto.CONFIRMADO)))
                .thenReturn(List.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividades));
    }

    private Proyecto proyecto(
            EstadoProyecto estado,
            LocalDateTime fechaInicio) {
        return Proyecto.builder()
                .idProyecto(100L)
                .nombre("Proyecto")
                .estado(estado)
                .fechaInicio(fechaInicio)
                .build();
    }

    private Planificacion planificacion(Proyecto proyecto) {
        return Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();
    }

    private Actividad actividad(
            Long id,
            String nombre,
            EstadoActividad estado,
            LocalDateTime inicio) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .estado(estado)
                .duracionMinutos(60)
                .fechaHoraInicio(inicio)
                .build();
    }
}
