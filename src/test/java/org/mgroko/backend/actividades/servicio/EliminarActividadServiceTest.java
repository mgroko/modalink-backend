package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.exception.ActividadNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteEliminarActividadesException;
import org.mgroko.backend.actividades.exception.MotivoEliminacionObligatorioException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private RequerimientoActividadService requerimientoActividadService;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @Mock
    private CronogramaService cronogramaService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EliminarActividadService service;

    private Usuario usuario;
    private Perfil perfil;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .fechaInicio(LocalDate.now())
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();

        actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("Montaje")
                .descripcion("Montaje del escenario")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad));
        when(asignacionActividadRepository.findByActividadIdConMiembroPerfil(55L))
                .thenReturn(List.of());
        when(postulacionActividadRepository.findByActividadIdConPerfil(55L))
                .thenReturn(List.of());
        when(invitacionActividadRepository.findByActividadIdConDestinatario(55L))
                .thenReturn(List.of());
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of());
    }

    @Test
    void eliminar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(1L, null, 100L, 55L, "Motivo"))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_perfilInexistente_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_perfilNoActivo_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PENDIENTE_BAJA);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(PerfilEnBajaException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_proyectoInexistente_lanzaExcepcion() {
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(ProyectoNoEncontradoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_sinPermiso_lanzaExcepcionYNoBuscaActividad() {
        doThrow(new AccesoDenegadoProyectoException("No tienes permiso."))
                .when(proyectoSecurityService)
                .validarPermiso(100L, 10L, EliminarActividadService.PERMISO_ELIMINAR_ACTIVIDAD);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(actividadRepository, never())
                .findByIdActividadAndPlanificacionIdPlanificacion(anyLong(), anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_proyectoFinalizado_lanzaExcepcion() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(EstadoProyectoNoPermiteEliminarActividadesException.class);

        verify(actividadRepository, never())
                .findByIdActividadAndPlanificacionIdPlanificacion(anyLong(), anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadInexistente_lanzaExcepcion() {
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(ActividadNoEncontradaException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadFinalizada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.FINALIZADA);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(ActividadNoEliminableException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadCancelada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.CANCELADA);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "Motivo"))
                .isInstanceOf(ActividadNoEliminableException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_motivoNulo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, null))
                .isInstanceOf(MotivoEliminacionObligatorioException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_motivoEnBlanco_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "   "))
                .isInstanceOf(MotivoEliminacionObligatorioException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_motivoDemasiadoLargo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 55L, "x".repeat(201)))
                .isInstanceOf(MotivoEliminacionObligatorioException.class)
                .hasMessageContaining("200");

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_conDatosValidos_eliminaHijosReconectaYPublicaEvento() {
        Actividad predecesora = Actividad.builder()
                .idActividad(54L)
                .nombre("Preparacion")
                .duracionMinutos(30)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        actividad.setPredecesoras(new HashSet<>(Set.of(predecesora)));

        Actividad sucesora = Actividad.builder()
                .idActividad(56L)
                .nombre("Cierre")
                .duracionMinutos(45)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        sucesora.setPredecesoras(new HashSet<>(Set.of(actividad)));

        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad, sucesora));

        Perfil perfilPostulante = Perfil.builder()
                .idPerfil(11L)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .build();
        Perfil perfilInvitado = Perfil.builder()
                .idPerfil(12L)
                .usuario(Usuario.builder().idUsuario(3L).build())
                .build();
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(70L)
                .actividad(actividad)
                .build();

        AsignacionActividad asignacion = AsignacionActividad.builder()
                .miembro(MiembroProyecto.builder()
                        .idMiembro(20L)
                        .perfil(perfil)
                        .estadoParticipacion(EstadoParticipacion.ACTIVO)
                        .build())
                .actividad(actividad)
                .build();

        PostulacionActividad postulacion = PostulacionActividad.builder()
                .idPostulacion(80L)
                .perfil(perfilPostulante)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        PostulacionActividad postulacionRechazada = PostulacionActividad.builder()
                .idPostulacion(81L)
                .perfil(Perfil.builder().idPerfil(13L).build())
                .estado(EstadoSolicitud.RECHAZADA)
                .build();

        InvitacionActividad invitacion = InvitacionActividad.builder()
                .idInvitacion(90L)
                .perfilDestinatario(perfilInvitado)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        InvitacionActividad invitacionRechazada = InvitacionActividad.builder()
                .idInvitacion(91L)
                .perfilDestinatario(Perfil.builder().idPerfil(14L).build())
                .estado(EstadoSolicitud.RECHAZADA)
                .build();

        when(asignacionActividadRepository.findByActividadIdConMiembroPerfil(55L))
                .thenReturn(List.of(asignacion));
        when(postulacionActividadRepository.findByActividadIdConPerfil(55L))
                .thenReturn(List.of(postulacion, postulacionRechazada));
        when(invitacionActividadRepository.findByActividadIdConDestinatario(55L))
                .thenReturn(List.of(invitacion, invitacionRechazada));
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of(requerimiento));

        service.eliminar(1L, 10L, 100L, 55L, "  Falta de presupuesto  ");

        assertThat(sucesora.getPredecesoras())
                .containsExactly(predecesora)
                .doesNotContain(actividad);

        verify(actividadRepository).deleteDependenciasByActividadId(55L);
        verify(actividadRepository).deleteHitoActividadByActividadId(55L);
        verify(postulacionActividadRepository).delete(postulacion);
        verify(postulacionActividadRepository).delete(postulacionRechazada);
        verify(invitacionActividadRepository).delete(invitacion);
        verify(invitacionActividadRepository).delete(invitacionRechazada);
        verify(requerimientoActividadService).eliminar(requerimiento);
        verify(asignacionActividadRepository).delete(asignacion);
        verify(actividadRepository).delete(actividad);
        verify(actividadRepository, atLeastOnce()).flush();
        // Fase 2: no se llama a recalcularTolerandoDesborde; las proyecciones no se persisten.
        verify(cronogramaService, never()).recalcularTolerandoDesborde(any(), any());

        ArgumentCaptor<ActividadEliminadaEvent> evento =
                ArgumentCaptor.forClass(ActividadEliminadaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().idActividad()).isEqualTo(55L);
        assertThat(evento.getValue().nombreActividad()).isEqualTo("Montaje");
        assertThat(evento.getValue().nombreProyecto()).isEqualTo("Desfile Primavera");
        assertThat(evento.getValue().motivo()).isEqualTo("Falta de presupuesto");
        assertThat(evento.getValue().idsPerfilesAsignados()).containsExactly(10L);
        assertThat(evento.getValue().postulaciones())
                .singleElement()
                .satisfies(afectada -> {
                    assertThat(afectada.idPostulacion()).isEqualTo(80L);
                    assertThat(afectada.idPerfil()).isEqualTo(11L);
                });
        assertThat(evento.getValue().invitaciones())
                .singleElement()
                .satisfies(afectada -> {
                    assertThat(afectada.idInvitacion()).isEqualTo(90L);
                    assertThat(afectada.idPerfil()).isEqualTo(12L);
                });
    }

    @Test
    void eliminar_sinAfectados_noPublicaEvento() {
        service.eliminar(1L, 10L, 100L, 55L, "Motivo");

        verify(actividadRepository).delete(actividad);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_sinFechaEntrega_omiteRecalculo() {
        planificacion.setFechaEntrega(null);

        service.eliminar(1L, 10L, 100L, 55L, "Motivo");

        verify(cronogramaService, never()).recalcularTolerandoDesborde(any(), any());
    }

    @Test
    void eliminar_sinActividadesRestantes_omiteRecalculo() {
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad))
                .thenReturn(List.of());

        service.eliminar(1L, 10L, 100L, 55L, "Motivo");

        verify(cronogramaService, never()).recalcularTolerandoDesborde(any(), any());
    }
}
