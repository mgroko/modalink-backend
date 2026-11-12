package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import org.mgroko.backend.actividades.dto.DecisionIntegranteRequest;
import org.mgroko.backend.actividades.dto.EliminarActividadRequest;
import org.mgroko.backend.actividades.dto.MiembroExclusivoResponse;
import org.mgroko.backend.actividades.enums.AccionIntegrante;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.exception.ActividadEnCursoConfirmadaNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadConReemplazosException;
import org.mgroko.backend.actividades.exception.ActividadNoEliminableException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.DecisionIntegranteInvalidaException;
import org.mgroko.backend.actividades.exception.EliminacionIntegranteNoPermitidaException;
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
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.EliminarIntegranteService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private ProyectoRepository proyectoRepository;
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
    private MiembrosActividadService miembrosActividadService;
    @Mock
    private EliminarIntegranteService eliminarIntegranteService;
    @Mock
    private CronogramaService cronogramaService;
    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;
    @Mock
    private org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository reemplazoRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EliminarActividadService service;

    private Perfil perfil;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(1L).build())
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
        when(proyectoRepository.findByIdParaGestion(100L))
                .thenReturn(Optional.of(proyecto));
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
        when(requerimientoActividadRepository.findByActividad_IdActividadIncludingArchived(55L))
                .thenReturn(List.of());
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of());
    }

    @Test
    void eliminar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(
                1L, null, 100L, 55L, request("Motivo")))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_perfilInexistente_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_perfilNoActivo_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PENDIENTE_BAJA);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(PerfilEnBajaException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_proyectoInexistente_lanzaExcepcion() {
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(ProyectoNoEncontradoException.class);

        verifyNoInteractions(proyectoSecurityService, eventPublisher);
    }

    @Test
    void eliminar_conReemplazosAsociados_lanzaExcepcionYNoBorra() {
        when(reemplazoRepository.existsByActividad_IdActividad(55L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(ActividadConReemplazosException.class);
    }

    @Test
    void eliminar_sinPermiso_lanzaExcepcionYNoBuscaActividad() {
        doThrow(new AccesoDenegadoProyectoException("No tienes permiso."))
                .when(proyectoSecurityService)
                .validarPermiso(100L, 10L, EliminarActividadService.PERMISO_ELIMINAR_ACTIVIDAD);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(actividadRepository, never())
                .findByIdActividadAndPlanificacionIdPlanificacion(anyLong(), anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_proyectoFinalizado_lanzaExcepcion() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(EstadoProyectoNoPermiteEliminarActividadesException.class);

        verify(actividadRepository, never())
                .findByIdActividadAndPlanificacionIdPlanificacion(anyLong(), anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_proyectoConfirmadoConActividadPendiente_permiteEliminar() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);

        service.eliminar(1L, 10L, 100L, 55L, request("Motivo"));

        verify(actividadRepository).delete(actividad);
    }

    @Test
    void eliminar_actividadEnCurso_lanzaExcepcion() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        actividad.setEstado(EstadoActividad.EN_CURSO);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(org.mgroko.backend.actividades.exception
                        .ActividadNoEliminableException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadInexistente_lanzaExcepcion() {
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(ActividadNoEncontradaException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadFinalizada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.FINALIZADA);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(ActividadNoEliminableException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_actividadCancelada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.CANCELADA);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("Motivo")))
                .isInstanceOf(ActividadNoEliminableException.class);

        verify(actividadRepository, never()).delete(any(Actividad.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void eliminar_motivoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request(null)))
                .isInstanceOf(MotivoEliminacionObligatorioException.class);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("   ")))
                .isInstanceOf(MotivoEliminacionObligatorioException.class);

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L, request("x".repeat(201))))
                .isInstanceOf(MotivoEliminacionObligatorioException.class);
    }

    @Test
    void eliminar_decisionesNulas_lanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L,
                new EliminarActividadRequest("Motivo", null)))
                .isInstanceOf(DecisionIntegranteInvalidaException.class);
    }

    @Test
    void eliminar_faltaDecisionDeUnExclusivo_lanzaExcepcion() {
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of(exclusivo(20L, true, null)));

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L,
                request("Motivo")))
                .isInstanceOf(DecisionIntegranteInvalidaException.class)
                .hasMessageContaining("Falta la decisión");
    }

    @Test
    void eliminar_decisionParaMiembroNoExclusivo_lanzaExcepcion() {
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L,
                new EliminarActividadRequest("Motivo",
                        List.of(new DecisionIntegranteRequest(
                                20L, AccionIntegrante.GUARDAR, null)))))
                .isInstanceOf(DecisionIntegranteInvalidaException.class)
                .hasMessageContaining("no participa exclusivamente");
    }

    @Test
    void eliminar_decisionDuplicada_lanzaExcepcion() {
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of(exclusivo(20L, true, null)));

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L,
                new EliminarActividadRequest("Motivo",
                        List.of(
                                new DecisionIntegranteRequest(
                                        20L, AccionIntegrante.GUARDAR, null),
                                new DecisionIntegranteRequest(
                                        20L, AccionIntegrante.GUARDAR, null)))))
                .isInstanceOf(DecisionIntegranteInvalidaException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    void eliminar_decisionEliminarNoPermitida_lanzaExcepcion() {
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of(exclusivo(20L, false, "AUTOELIMINACION")));

        assertThatThrownBy(() -> service.eliminar(
                1L, 10L, 100L, 55L,
                new EliminarActividadRequest("Motivo",
                        List.of(new DecisionIntegranteRequest(
                                20L, AccionIntegrante.ELIMINAR, null)))))
                .isInstanceOf(EliminacionIntegranteNoPermitidaException.class);
    }

    @Test
    void eliminar_conDecisiones_aplicaBajasYConservaAlGuardado() {
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
        when(miembrosActividadService.listarExclusivos(1L, 10L, 100L, 55L))
                .thenReturn(List.of(
                        exclusivo(20L, true, null),
                        exclusivo(21L, true, null)));

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

        AsignacionActividad asignacionGuardada = AsignacionActividad.builder()
                .miembro(MiembroProyecto.builder()
                        .idMiembro(21L)
                        .perfil(perfil)
                        .estadoParticipacion(EstadoParticipacion.ACTIVO)
                        .build())
                .actividad(actividad)
                .build();
        AsignacionActividad asignacionEliminada = AsignacionActividad.builder()
                .miembro(MiembroProyecto.builder()
                        .idMiembro(20L)
                        .perfil(Perfil.builder().idPerfil(11L).build())
                        .estadoParticipacion(EstadoParticipacion.ELIMINADO)
                        .build())
                .actividad(actividad)
                .build();

        PostulacionActividad postulacion = PostulacionActividad.builder()
                .idPostulacion(80L)
                .perfil(perfilPostulante)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        InvitacionActividad invitacion = InvitacionActividad.builder()
                .idInvitacion(90L)
                .perfilDestinatario(perfilInvitado)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        when(asignacionActividadRepository.findByActividadIdConMiembroPerfil(55L))
                .thenReturn(List.of(asignacionGuardada, asignacionEliminada));
        when(postulacionActividadRepository.findByActividadIdConPerfil(55L))
                .thenReturn(List.of(postulacion));
        when(invitacionActividadRepository.findByActividadIdConDestinatario(55L))
                .thenReturn(List.of(invitacion));
        when(requerimientoActividadRepository.findByActividad_IdActividadIncludingArchived(55L))
                .thenReturn(List.of(requerimiento));

        EliminarActividadRequest request = new EliminarActividadRequest(
                "  Falta de presupuesto  ",
                List.of(
                        new DecisionIntegranteRequest(
                                20L, AccionIntegrante.ELIMINAR, null),
                        new DecisionIntegranteRequest(
                                21L, AccionIntegrante.GUARDAR, null)));

        service.eliminar(1L, 10L, 100L, 55L, request);

        verify(eliminarIntegranteService).eliminar(
                eq(1L),
                eq(10L),
                eq(100L),
                eq(20L),
                eq(new EliminarIntegranteRequest("Falta de presupuesto")));
        verify(eliminarIntegranteService, never()).eliminar(
                eq(1L), eq(10L), eq(100L), eq(21L), any(EliminarIntegranteRequest.class));

        assertThat(sucesora.getPredecesoras())
                .containsExactly(predecesora)
                .doesNotContain(actividad);

        verify(actividadRepository).deleteDependenciasByActividadId(55L);
        verify(actividadRepository).deleteHitoActividadByActividadId(55L);
        verify(postulacionActividadRepository).delete(postulacion);
        verify(invitacionActividadRepository).delete(invitacion);
        verify(requerimientoActividadService).eliminarFisico(requerimiento);
        verify(asignacionActividadRepository).delete(asignacionGuardada);
        verify(asignacionActividadRepository).delete(asignacionEliminada);
        verify(actividadRepository).delete(actividad);

        InOrder ordenBorrado = inOrder(
                postulacionActividadRepository,
                invitacionActividadRepository,
                asignacionActividadRepository,
                requerimientoActividadService,
                actividadRepository);
        ordenBorrado.verify(postulacionActividadRepository).delete(postulacion);
        ordenBorrado.verify(invitacionActividadRepository).delete(invitacion);
        ordenBorrado.verify(asignacionActividadRepository).delete(asignacionGuardada);
        ordenBorrado.verify(asignacionActividadRepository).flush();
        ordenBorrado.verify(requerimientoActividadService).eliminarFisico(requerimiento);
        ordenBorrado.verify(actividadRepository).delete(actividad);

        ArgumentCaptor<ActividadEliminadaEvent> evento =
                ArgumentCaptor.forClass(ActividadEliminadaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().motivo()).isEqualTo("Falta de presupuesto");
        assertThat(evento.getValue().idsPerfilesAsignados()).containsExactly(10L);
        assertThat(evento.getValue().postulaciones()).hasSize(1);
        assertThat(evento.getValue().invitaciones()).hasSize(1);
    }

    @Test
    void eliminar_sinAfectados_noPublicaEvento() {
        service.eliminar(1L, 10L, 100L, 55L, request("Motivo"));

        verify(actividadRepository).delete(actividad);
        verifyNoInteractions(eventPublisher);
    }

    private EliminarActividadRequest request(String motivo) {
        return new EliminarActividadRequest(motivo, List.of());
    }

    private MiembroExclusivoResponse exclusivo(
            Long idMiembro,
            boolean puedeEliminar,
            String motivoNoEliminable) {
        return new MiembroExclusivoResponse(
                30L,
                idMiembro,
                20L + idMiembro,
                30L + idMiembro,
                "Miembro " + idMiembro,
                2L,
                "Modelo",
                puedeEliminar,
                motivoNoEliminable);
    }
}
