package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.DesasignacionActividadResponse;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.AsignacionActividadNoPermitidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesasignarActividadServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_ACTIVIDAD = 55L;
    private static final Long ID_PERFIL_DESTINO = 12L;
    private static final Long ID_MIEMBRO = 900L;
    private static final Long ID_ASIGNACION = 1200L;
    private static final Long ID_REQUERIMIENTO = 123L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private NotificacionService notificacionService;

    private DesasignarActividadService service;

    private Perfil actor;
    private Perfil destino;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;
    private MiembroProyecto miembro;
    private RequerimientoActividad requerimiento;
    private AsignacionActividad asignacion;

    @BeforeEach
    void setUp() {
        service = new DesasignarActividadService(
                perfilRepository,
                planificacionRepository,
                actividadRepository,
                asignacionActividadRepository,
                cupoRequerimientoService,
                proyectoSecurityService,
                notificacionService);

        actor = Perfil.builder()
                .idPerfil(ID_PERFIL_ACTOR)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(ID_USUARIO).build())
                .build();

        destino = Perfil.builder()
                .idPerfil(ID_PERFIL_DESTINO)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(2L)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(7L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 12, 31))
                .build();

        actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Montaje")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        miembro = MiembroProyecto.builder()
                .idMiembro(ID_MIEMBRO)
                .proyecto(proyecto)
                .perfil(destino)
                .rolProyecto(RolProyecto.builder().codigo("MIEMBRO").build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .actividad(actividad)
                .profesion(destino.getProfesion())
                .cantidad(1)
                .build();

        asignacion = AsignacionActividad.builder()
                .idAsignacionAct(ID_ASIGNACION)
                .miembro(miembro)
                .actividad(actividad)
                .requerimientoActividad(requerimiento)
                .build();
    }

    @Test
    void desasignaBorraNotificaYLiberaCupo() {
        prepararEscenarioBase();
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(notificacionService.crear(
                any(), any(), eq(NotificacionService.TIPO_ACTIVIDAD_DESASIGNADA),
                any(), any(), anyLong(), anyLong()))
                .thenReturn(Notificacion.builder().build());

        DesasignacionActividadResponse response = service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION);

        assertThat(response.idAsignacionActividad()).isEqualTo(ID_ASIGNACION);
        assertThat(response.idRequerimientoActividad()).isEqualTo(ID_REQUERIMIENTO);
        assertThat(response.requerimientoSatisfecho()).isFalse();
        assertThat(response.cupo().faltantes()).isEqualTo(1);
        assertThat(response.advertencias())
                .contains("El requerimiento quedó con 1 vacante(s).");

        verify(asignacionActividadRepository).delete(asignacion);
        verify(asignacionActividadRepository).flush();
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void noRequiereFechaDeEntrega() {
        planificacion.setFechaEntrega(null);
        asignacion.setRequerimientoActividad(null);
        prepararEscenarioBase();
        when(notificacionService.crear(
                any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(Notificacion.builder().build());

        DesasignacionActividadResponse response = service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION);

        assertThat(response.idAsignacionActividad()).isEqualTo(ID_ASIGNACION);
        assertThat(response.cupo()).isNull();
        verify(cupoRequerimientoService, never()).calcularActividad(any());
    }

    @Test
    void proyectoConfirmadoDevuelveAdvertencia() {
        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        prepararEscenarioBase();
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));
        when(notificacionService.crear(
                any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(Notificacion.builder().build());

        DesasignacionActividadResponse response = service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION);

        assertThat(response.advertencias())
                .contains("El proyecto está confirmado; la desasignación no reprograma el cronograma.");
    }

    @Test
    void rechazaAsignacionInexistente() {
        prepararEscenarioBase();
        when(asignacionActividadRepository.findByIdParaDesasignar(ID_ASIGNACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AsignacionActividadNoEncontradaException.class);

        verify(asignacionActividadRepository, never()).delete(any());
    }

    @Test
    void rechazaAsignacionDeOtraActividad() {
        prepararEscenarioBase();
        Actividad otra = Actividad.builder()
                .idActividad(99L)
                .planificacion(planificacion)
                .build();
        asignacion.setActividad(otra);

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AsignacionActividadNoEncontradaException.class);
    }

    @Test
    void rechazaMiembroHistorico() {
        prepararEscenarioBase();
        miembro.setEstadoParticipacion(EstadoParticipacion.ELIMINADO);

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("histórica");
    }

    @Test
    void rechazaProyectoFinalizado() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        prepararEscenarioBase();

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("no admite desasignaciones");
    }

    @Test
    void rechazaActividadFinalizada() {
        actividad.setEstado(EstadoActividad.FINALIZADA);
        prepararEscenarioBase();

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AsignacionActividadNoPermitidaException.class)
                .hasMessageContaining("no admite desasignaciones");
    }

    @Test
    void rechazaSinPermiso() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(actor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(ID_PROYECTO, ID_PERFIL_ACTOR,
                        AsignarActividadService.PERMISO_ASIGNAR_ACTIVIDAD);

        assertThatThrownBy(() -> service.desasignar(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD, ID_ASIGNACION))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private void prepararEscenarioBase() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(actor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(
                ID_ACTIVIDAD, planificacion.getIdPlanificacion()))
                .thenReturn(Optional.of(actividad));
        when(asignacionActividadRepository.findByIdParaDesasignar(ID_ASIGNACION))
                .thenReturn(Optional.of(asignacion));
    }
}
