package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.exception.ActividadSinFechaInicioException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CambiarEstadoActividadServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private ObtenerActividadService obtenerActividadService;
    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CambiarEstadoActividadService service;

    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .estado(EstadoPerfil.Activo)
                .build();
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile")
                .estado(EstadoProyecto.CONFIRMADO)
                .fechaInicio(LocalDateTime.of(2026, 11, 1, 9, 0))
                .build();
        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 30))
                .build();
        actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("Montaje")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 2, 10, 0))
                .planificacion(planificacion)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.saveAndFlush(any(Actividad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad));
        when(asignacionActividadRepository.findActivasByActividadId(55L))
                .thenReturn(List.of());
    }

    @Test
    void cambiar_pendienteAEnCurso_actualizaEstado() {
        service.cambiar(1L, 10L, 100L, 55L, EstadoActividad.EN_CURSO);

        assertThat(actividad.getEstado()).isEqualTo(EstadoActividad.EN_CURSO);
        verify(actividadRepository).saveAndFlush(actividad);
    }

    @Test
    void cambiar_sinFechaInicio_lanzaExcepcion() {
        actividad.setFechaHoraInicio(null);

        assertThatThrownBy(() -> service.cambiar(
                1L, 10L, 100L, 55L, EstadoActividad.EN_CURSO))
                .isInstanceOf(ActividadSinFechaInicioException.class);
    }

    @Test
    void cambiar_aCancelada_reprogramaElProyecto() {
        service.cambiar(1L, 10L, 100L, 55L, EstadoActividad.CANCELADA);

        assertThat(actividad.getEstado()).isEqualTo(EstadoActividad.CANCELADA);
        verify(reprogramarProyectoService).reprogramar(
                100L, MotivoReprogramacion.CANCELACION_ACTIVIDAD, null);
    }
}
