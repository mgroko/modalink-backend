package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.ReprogramacionProyectoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReprogramarProyectoServiceTest {

    private static final LocalDateTime INICIO_PROYECTO =
            LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final LocalDateTime INICIO_VIEJO =
            LocalDateTime.of(2026, 11, 2, 9, 0);
    private static final LocalDateTime INICIO_NUEVO =
            LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final LocalDateTime INICIO_FIJO =
            LocalDateTime.of(2026, 11, 2, 8, 0);

    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private PlanificadorRecursosService planificadorRecursosService;
    @Mock
    private CalendarioService calendarioService;
    @Mock
    private CronogramaService cronogramaService;

    private ReprogramarProyectoService service;

    @BeforeEach
    void setUp() {
        service = new ReprogramarProyectoService(
                proyectoRepository,
                planificacionRepository,
                actividadRepository,
                asignacionActividadRepository,
                planificadorRecursosService,
                calendarioService,
                cronogramaService);
    }

    @Test
    void reprogramar_pendienteYEnCurso_persisteNuevosIniciosYConservaFija() {
        Proyecto proyecto = proyectoConfirmado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad pendiente = actividad(
                1L, "Pendiente", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);
        Actividad fija = actividad(
                2L, "En curso", 60, EstadoActividad.EN_CURSO, INICIO_FIJO);

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(pendiente, fija));
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO), eq(true), eq(true),
                anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, INICIO_NUEVO, 2L, INICIO_FIJO),
                        Map.of(),
                        null));

        ReprogramacionProyectoResponse resultado = service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null);

        assertThat(pendiente.getFechaHoraInicio()).isEqualTo(INICIO_NUEVO);
        assertThat(fija.getFechaHoraInicio()).isEqualTo(INICIO_FIJO);
        assertThat(resultado.actividades()).hasSize(1);
        assertThat(resultado.actividades().get(0).idActividad()).isEqualTo(1L);
        assertThat(resultado.actividades().get(0).inicioAnterior()).isEqualTo(INICIO_VIEJO);
        assertThat(resultado.actividades().get(0).inicioNuevo()).isEqualTo(INICIO_NUEVO);
        verify(actividadRepository, atLeastOnce()).saveAll(any());
        verify(actividadRepository, atLeastOnce()).flush();
    }

    @Test
    void reprogramar_proyectoFinalizado_lanzaExcepcion() {
        Proyecto proyecto = proyectoConfirmado();
        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));

        assertThatThrownBy(() -> service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("ESTADO_PROYECTO_INVALIDO");
    }

    @Test
    void reprogramar_sinFechaInicio_lanzaExcepcion() {
        Proyecto proyecto = proyectoConfirmado();
        proyecto.setFechaInicio((LocalDateTime) null);
        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion(proyecto)));

        assertThatThrownBy(() -> service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("DATOS_INCONSISTENTES");
    }

    @Test
    void reprogramar_fijaFueraDeDisponibilidad_reportaConflictoSinMoverla() {
        Proyecto proyecto = proyectoConfirmado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad fija = actividad(
                2L, "En curso", 60, EstadoActividad.EN_CURSO, INICIO_FIJO);
        AsignacionActividad asignacion = asignacion(10L, 20L, 30L, fija);

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(fija));
        when(asignacionActividadRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(asignacion));
        when(calendarioService.ventanaDisponiblePara(
                eq(List.of(30L)), eq(INICIO_FIJO),
                eq(INICIO_FIJO.plusMinutes(60)), eq(2L)))
                .thenReturn(false);

        ReprogramacionProyectoResponse resultado = service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null);

        assertThat(fija.getFechaHoraInicio()).isEqualTo(INICIO_FIJO);
        assertThat(resultado.actividades()).isEmpty();
        assertThat(resultado.conflictosFijos()).hasSize(1);
        assertThat(resultado.conflictosFijos().get(0).idActividad()).isEqualTo(2L);
    }

    @Test
    void reprogramar_conNuevaEntrega_actualizaLaPlanificacion() {
        Proyecto proyecto = proyectoConfirmado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad pendiente = actividad(
                1L, "Pendiente", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);
        LocalDate nuevaEntrega = LocalDate.of(2026, 12, 20);

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(pendiente));
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO), eq(true), eq(true),
                anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, INICIO_NUEVO), Map.of(), null));

        ReprogramacionProyectoResponse resultado = service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_ENTREGA, nuevaEntrega);

        assertThat(planificacion.getFechaEntrega()).isEqualTo(nuevaEntrega);
        assertThat(resultado.fechaEntrega()).isEqualTo(nuevaEntrega);
        verify(planificacionRepository).save(planificacion);
    }

    @Test
    void reprogramarActividad_noMuevePredecesorasYDesplazaSucesorasPendientes() {
        Proyecto proyecto = proyectoConfirmado();
        Planificacion planificacion = planificacion(proyecto);

        Actividad predecesora = actividad(
                1L, "Predecesora", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);
        Actividad objetivo = actividad(
                2L, "Objetivo", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO.plusHours(1));
        objetivo.setPredecesoras(Set.of(predecesora));
        Actividad sucesora = actividad(
                3L, "Sucesora", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO.plusHours(2));
        sucesora.setPredecesoras(Set.of(objetivo));

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(predecesora, objetivo, sucesora));
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO),
                eq(true), eq(true), anyMap(), anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(
                                1L, INICIO_VIEJO,
                                2L, INICIO_VIEJO.plusHours(2),
                                3L, INICIO_VIEJO.plusHours(3)),
                        Map.of(),
                        null));

        service.reprogramarActividad(
                100L,
                2L,
                MotivoReprogramacion.ASIGNACION,
                Map.of(2L, List.of(30L)));

        assertThat(predecesora.getFechaHoraInicio()).isEqualTo(INICIO_VIEJO);
        assertThat(objetivo.getFechaHoraInicio()).isEqualTo(INICIO_VIEJO.plusHours(2));
        assertThat(sucesora.getFechaHoraInicio()).isEqualTo(INICIO_VIEJO.plusHours(3));
    }

    @Test
    void simularAsignacion_devuelvePropuestaYNoModificaLaActividad() {
        Proyecto proyecto = proyectoConfirmado();
        Planificacion planificacion = planificacion(proyecto);

        Actividad predecesora = actividad(
                1L, "Predecesora", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);
        Actividad objetivo = actividad(
                2L, "Objetivo", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO.plusHours(1));
        objetivo.setPredecesoras(Set.of(predecesora));

        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(predecesora, objetivo));
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO),
                eq(true), eq(true), anyMap(), anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, INICIO_VIEJO, 2L, INICIO_VIEJO.plusHours(3)),
                        Map.of(),
                        null));

        ReprogramarProyectoService.SimulacionAsignacion simulacion =
                service.simularAsignacion(100L, 2L, List.of(30L));

        assertThat(simulacion.inicioPropuesto()).isEqualTo(INICIO_VIEJO.plusHours(3));
        assertThat(simulacion.actividadesAfectadas()).hasSize(1);
        assertThat(objetivo.getFechaHoraInicio()).isEqualTo(INICIO_VIEJO.plusHours(1));
    }

    @Test
    void reprogramar_proyectoPublicado_usaInicioEfectivoYReplanifica() {
        Proyecto proyecto = proyectoPublicado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad pendiente = actividad(
                1L, "Pendiente", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(pendiente));
        when(cronogramaService.inicioEfectivo(planificacion))
                .thenReturn(INICIO_PROYECTO);
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO),
                eq(true), eq(true), anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, INICIO_NUEVO),
                        Map.of(),
                        null));

        ReprogramacionProyectoResponse resultado = service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null);

        assertThat(pendiente.getFechaHoraInicio()).isEqualTo(INICIO_NUEVO);
        assertThat(resultado.actividades()).hasSize(1);
        verify(cronogramaService).inicioEfectivo(planificacion);
    }

    @Test
    void reprogramar_proyectoBorrador_lanzaExcepcion() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Borrador")
                .estado(EstadoProyecto.BORRADOR)
                .build();

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));

        assertThatThrownBy(() -> service.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("ESTADO_PROYECTO_INVALIDO");
    }

    @Test
    void reprogramarActividad_proyectoPublicado_mueveLaActividad() {
        Proyecto proyecto = proyectoPublicado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad otra = actividad(
                1L, "Otra", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);
        Actividad objetivo = actividad(
                2L, "Objetivo", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO.plusHours(1));

        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(otra, objetivo));
        when(cronogramaService.inicioEfectivo(planificacion))
                .thenReturn(INICIO_PROYECTO);
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO),
                eq(true), eq(true), anyMap(), anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, INICIO_VIEJO, 2L, INICIO_NUEVO),
                        Map.of(),
                        null));

        service.reprogramarActividad(
                100L,
                2L,
                MotivoReprogramacion.ASIGNACION,
                Map.of(2L, List.of(30L)));

        assertThat(objetivo.getFechaHoraInicio()).isEqualTo(INICIO_NUEVO);
    }

    @Test
    void simularAsignacion_proyectoPublicado_devuelveSimulacion() {
        Proyecto proyecto = proyectoPublicado();
        Planificacion planificacion = planificacion(proyecto);
        Actividad objetivo = actividad(
                2L, "Objetivo", 60, EstadoActividad.PENDIENTE, INICIO_VIEJO);

        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(objetivo));
        when(cronogramaService.inicioEfectivo(planificacion))
                .thenReturn(INICIO_PROYECTO);
        when(planificadorRecursosService.planificar(
                eq(planificacion), anyList(), eq(INICIO_PROYECTO),
                eq(true), eq(true), anyMap(), anyMap()))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(2L, INICIO_NUEVO),
                        Map.of(),
                        null));

        ReprogramarProyectoService.SimulacionAsignacion simulacion =
                service.simularAsignacion(100L, 2L, List.of(30L));

        assertThat(simulacion.inicioPropuesto()).isEqualTo(INICIO_NUEVO);
        assertThat(objetivo.getFechaHoraInicio()).isEqualTo(INICIO_VIEJO);
    }

    private Proyecto proyectoConfirmado() {
        return Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.CONFIRMADO)
                .fechaInicio(INICIO_PROYECTO)
                .build();
    }

    private Proyecto proyectoPublicado() {
        return Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
    }

    private Planificacion planificacion(Proyecto proyecto) {
        return Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 12, 15))
                .build();
    }

    private Actividad actividad(
            Long id,
            String nombre,
            int duracion,
            EstadoActividad estado,
            LocalDateTime inicio) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .duracionMinutos(duracion)
                .estado(estado)
                .fechaHoraInicio(inicio)
                .build();
    }

    private AsignacionActividad asignacion(
            Long idAsignacion,
            Long idPerfil,
            Long idUsuario,
            Actividad actividad) {
        Usuario usuario = Usuario.builder().idUsuario(idUsuario).build();
        Perfil perfil = Perfil.builder().idPerfil(idPerfil).usuario(usuario).build();
        MiembroProyecto miembro = MiembroProyecto.builder()
                .idMiembro(idPerfil + 100)
                .perfil(perfil)
                .build();
        return AsignacionActividad.builder()
                .idAsignacionAct(idAsignacion)
                .miembro(miembro)
                .actividad(actividad)
                .build();
    }
}
