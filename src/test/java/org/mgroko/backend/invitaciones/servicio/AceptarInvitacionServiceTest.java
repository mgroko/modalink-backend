package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.servicio.EvaluarDisponibilidadActividadService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.invitaciones.dto.InvitacionGestionadaResponse;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
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
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AceptarInvitacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_INVITACION = 100L;
    private static final Long ID_REQUERIMIENTO = 30L;
    private static final Long ID_ACTIVIDAD = 40L;

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private RolProyectoRepository rolProyectoRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private EvaluarDisponibilidadActividadService evaluarDisponibilidadActividadService;
    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AceptarInvitacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AceptarInvitacionService(
                perfilRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                requerimientoGralProyectoRepository,
                requerimientoActividadRepository,
                miembroProyectoRepository,
                asignacionActividadRepository,
                rolProyectoRepository,
                cupoRequerimientoService,
                evaluarDisponibilidadActividadService,
                reprogramarProyectoService,
                notificacionService,
                eventPublisher,
                RELOJ);

        prepararPerfilActivo();
        prepararNotificacion();
    }

    @Test
    void generalSinRequerimiento_creaMembresiaYAceptaInvitacion() {
        InvitacionGral invitacion = generalSinRequerimiento();
        when(invitacionGralRepository.findById(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionGralRepository.saveAndFlush(invitacion))
                .thenReturn(invitacion);
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        any(), any(), any()))
                .thenReturn(false);
        when(rolProyectoRepository.findByCodigo("MIEMBRO"))
                .thenReturn(Optional.of(RolProyecto.builder()
                        .idRolProyecto(2L)
                        .codigo("MIEMBRO")
                        .nombre("Miembro de proyecto")
                        .build()));
        when(miembroProyectoRepository.saveAndFlush(any(MiembroProyecto.class)))
                .thenAnswer(invocacion -> {
                    MiembroProyecto miembro = invocacion.getArgument(0);
                    miembro.setIdMiembro(88L);
                    return miembro;
                });

        InvitacionGestionadaResponse respuesta = servicio.aceptarGeneral(
                ID_USUARIO, ID_PERFIL, ID_INVITACION);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isEqualTo(88L);
        assertThat(invitacion.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
    }

    @Test
    void actividadSinDisponibilidad_lanzaConflicto() {
        InvitacionActividad invitacion = actividadPendiente();
        RequerimientoActividad requerimiento = invitacion.getRequerimiento();

        when(invitacionActividadRepository.findById(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionActividadRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(requerimientoActividadRepository.findByIdParaGestion(ID_REQUERIMIENTO))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL))
                .thenReturn(Optional.empty());
        when(asignacionActividadRepository
                .existsAsignacionActivaDeUsuarioEnActividad(any(), any()))
                .thenReturn(false);
        when(asignacionActividadRepository.findActivasByActividadId(ID_ACTIVIDAD))
                .thenReturn(List.of());
        when(evaluarDisponibilidadActividadService.evaluar(any(), any(), any()))
                .thenReturn(new ResultadoDisponibilidad(
                        false, false,
                        LocalDateTime.of(2026, 11, 10, 9, 0),
                        LocalDateTime.of(2026, 11, 10, 17, 0))
                        .conCodigoMotivo("SIN_HUECO"));

        assertThatThrownBy(() -> servicio.aceptarActividad(
                ID_USUARIO, ID_PERFIL, ID_INVITACION))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("disponibilidad");
    }

    private void prepararPerfilActivo() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL)));
    }

    private void prepararNotificacion() {
        when(notificacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .usuarioDestino(invocacion.getArgument(0))
                        .perfilContexto(invocacion.getArgument(1))
                        .tipo(invocacion.getArgument(2))
                        .mensaje(invocacion.getArgument(3))
                        .entidadTipo(invocacion.getArgument(4))
                        .entidadId(invocacion.getArgument(5))
                        .idProyecto(invocacion.getArgument(6))
                        .build());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());
    }

    private Perfil perfil(Long id) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico("Perfil " + id)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder()
                        .idUsuario(ID_USUARIO)
                        .estado(EstadoUsuario.ACTIVO)
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .build();
    }

    private Proyecto proyecto() {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto UC28")
                .descripcion("Descripción")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
    }

    private InvitacionGral generalSinRequerimiento() {
        return InvitacionGral.builder()
                .idInvitacionGral(ID_INVITACION)
                .proyecto(proyecto())
                .perfilRemitente(perfil(20L))
                .perfilDestinatario(perfil(ID_PERFIL))
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
    }

    private InvitacionActividad actividadPendiente() {
        Actividad actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Actividad UC28")
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .proyecto(proyecto())
                        .build())
                .build();

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(1)
                .descripcion("Requerimiento UC28")
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .actividad(actividad)
                .build();

        return InvitacionActividad.builder()
                .idInvitacion(ID_INVITACION)
                .perfilRemitente(perfil(20L))
                .perfilDestinatario(perfil(ID_PERFIL))
                .requerimiento(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
    }
}
