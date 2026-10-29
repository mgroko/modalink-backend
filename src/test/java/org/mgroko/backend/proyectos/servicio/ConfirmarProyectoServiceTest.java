package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.evento.ProyectoConfirmadoEvent;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class ConfirmarProyectoServiceTest {

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 1, 9, 0);

    private ContextoConfirmacionProyectoService contextoService;
    private PlanificadorRecursosService planificador;
    private ProyectoRepository proyectoRepository;
    private ActividadRepository actividadRepository;
    private ObtenerProyectoService obtenerProyectoService;
    private ObtenerCronogramaService obtenerCronogramaService;
    private ApplicationEventPublisher eventPublisher;
    private ConfirmarProyectoService service;

    @BeforeEach
    void setUp() {
        contextoService = Mockito.mock(ContextoConfirmacionProyectoService.class);
        planificador = Mockito.mock(PlanificadorRecursosService.class);
        proyectoRepository = Mockito.mock(ProyectoRepository.class);
        actividadRepository = Mockito.mock(ActividadRepository.class);
        obtenerProyectoService = Mockito.mock(ObtenerProyectoService.class);
        obtenerCronogramaService = Mockito.mock(ObtenerCronogramaService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        service = new ConfirmarProyectoService(
                contextoService,
                planificador,
                proyectoRepository,
                actividadRepository,
                obtenerProyectoService,
                obtenerCronogramaService,
                eventPublisher);
    }

    @Test
    void confirmar_caminoFeliz_persisteEstadoFechaYHorarios() {
        Actividad actividad = Actividad.builder()
                .idActividad(1L)
                .nombre("Montaje")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .build();
        Proyecto proyecto = proyecto();
        Planificacion planificacion = planificacion(proyecto);
        ContextoConfirmacionProyectoService.Contexto contexto = contexto(
                proyecto, planificacion, List.of(actividad));

        when(contextoService.cargar(1L, 10L, 100L, true))
                .thenReturn(contexto);
        when(proyectoRepository.saveAndFlush(any(Proyecto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(planificador.planificar(
                eq(planificacion), anyList(), eq(AHORA), eq(true)))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(1L, AHORA.plusHours(1)),
                        Map.of(),
                        new CalculoCpm(Map.of(), 60, 1440L, true, List.of())));
        when(actividadRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmar(1L, 10L, 100L);

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.CONFIRMADO);
        assertThat(proyecto.getFechaInicio()).isEqualTo(AHORA);
        assertThat(actividad.getFechaHoraInicio()).isEqualTo(AHORA.plusHours(1));
        assertThat(actividad.getOrigenInicio()).isEqualTo(OrigenInicio.CONFIRMADA);
        verify(eventPublisher).publishEvent(any(ProyectoConfirmadoEvent.class));
        verify(actividadRepository).flush();
    }

    @Test
    void confirmar_conBloqueos_lanzaExcepcionSinPersistir() {
        Proyecto proyecto = proyecto();
        Planificacion planificacion = planificacion(proyecto);
        ContextoConfirmacionProyectoService.Contexto contexto =
                new ContextoConfirmacionProyectoService.Contexto(
                        proyecto,
                        planificacion,
                        AHORA,
                        List.of(),
                        List.of(),
                        new RequerimientoCompletitudService.Resultado(
                                List.of(), List.of(), List.of()),
                        List.of(),
                        List.of(new BloqueoConfirmacionResponse(
                                "REQUERIMIENTOS_INCOMPLETOS",
                                "Falta personal.",
                                "PROYECTO",
                                100L)));

        when(contextoService.cargar(1L, 10L, 100L, true))
                .thenReturn(contexto);

        assertThatThrownBy(() -> service.confirmar(1L, 10L, 100L))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("REQUERIMIENTOS_INCOMPLETOS");
        verifyNoInteractions(proyectoRepository);
    }

    @Test
    void confirmar_sinHorarioParaUnaActividad_lanzaExcepcion() {
        Actividad actividad = Actividad.builder()
                .idActividad(1L)
                .nombre("Montaje")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .build();
        Proyecto proyecto = proyecto();
        Planificacion planificacion = planificacion(proyecto);
        ContextoConfirmacionProyectoService.Contexto contexto = contexto(
                proyecto, planificacion, List.of(actividad));

        when(contextoService.cargar(1L, 10L, 100L, true))
                .thenReturn(contexto);
        when(proyectoRepository.saveAndFlush(any(Proyecto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(planificador.planificar(
                eq(planificacion), anyList(), eq(AHORA), eq(true)))
                .thenReturn(new PlanificadorRecursosService.Resultado(
                        Map.of(),
                        Map.of(),
                        new CalculoCpm(Map.of(), 60, 1440L, true, List.of())));

        assertThatThrownBy(() -> service.confirmar(1L, 10L, 100L))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("DATOS_INCONSISTENTES");
    }

    private Proyecto proyecto() {
        return Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile")
                .descripcion("Descripción")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
    }

    private Planificacion planificacion(Proyecto proyecto) {
        return Planificacion.builder()
                .idPlanificacion(200L)
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .proyecto(proyecto)
                .build();
    }

    private ContextoConfirmacionProyectoService.Contexto contexto(
            Proyecto proyecto,
            Planificacion planificacion,
            List<Actividad> actividades) {
        return new ContextoConfirmacionProyectoService.Contexto(
                proyecto,
                planificacion,
                AHORA,
                actividades,
                actividades,
                new RequerimientoCompletitudService.Resultado(
                        List.of(), List.of(), List.of()),
                List.of(),
                List.of());
    }
}
