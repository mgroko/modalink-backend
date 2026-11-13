package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RechazoAutomaticoCupoServiceTest {

    private static final Long ID_REQUERIMIENTO_ACT = 30L;
    private static final Long ID_REQUERIMIENTO_GRAL = 40L;
    private static final Long ID_PROYECTO = 5L;

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private NotificacionRepository notificacionRepository;

    private RechazoAutomaticoCupoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new RechazoAutomaticoCupoService(
                cupoRequerimientoService,
                requerimientoActividadRepository,
                requerimientoGralProyectoRepository,
                postulacionActividadRepository,
                postulacionGralRepository,
                invitacionActividadRepository,
                invitacionGralRepository,
                notificacionService,
                notificacionRepository,
                RELOJ);

        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .usuarioDestino(invocacion.getArgument(0))
                        .perfilContexto(invocacion.getArgument(1))
                        .tipo(invocacion.getArgument(2))
                        .mensaje(invocacion.getArgument(3))
                        .entidadTipo(invocacion.getArgument(4))
                        .entidadId(invocacion.getArgument(5))
                        .idProyecto(invocacion.getArgument(6))
                        .build());
    }

    @Test
    void actividad_cupoCompleto_rechazaPostulacionEInvitacionYNotifica() {
        RequerimientoActividad requerimiento = requerimientoActividad();
        PostulacionActividad postulacion = postulacionActividad(11L, 1L);
        InvitacionActividad invitacion = invitacionActividad(21L, 2L);

        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));
        when(postulacionActividadRepository
                .findPendientesPorRequerimientoParaActualizar(ID_REQUERIMIENTO_ACT))
                .thenReturn(List.of(postulacion));
        when(invitacionActividadRepository
                .findPendientesPorRequerimientoParaActualizar(ID_REQUERIMIENTO_ACT))
                .thenReturn(List.of(invitacion));
        when(notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadId(
                        any(), anyString(), anyString(), any())).thenReturn(false);

        int rechazadas = servicio.rechazarPendientesSiCupoCompletoActividad(ID_REQUERIMIENTO_ACT);

        assertThat(rechazadas).isEqualTo(2);
        assertThat(postulacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(invitacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(invitacion.getFechaRechazo()).isNotNull();

        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(Notificacion::getTipo)
                .containsExactly(
                        NotificacionService.TIPO_POSTULACION_RECHAZADA,
                        NotificacionService.TIPO_INVITACION_RECHAZADA);
        assertThat(captor.getValue())
                .extracting(Notificacion::getMensaje)
                .allMatch(mensaje -> mensaje.contains("cupo ya fue cubierto"));
    }

    @Test
    void actividad_cupoNoCompleto_noRechazaNiNotifica() {
        RequerimientoActividad requerimiento = requerimientoActividad();

        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(2, 1, 1, false));

        int rechazadas = servicio.rechazarPendientesSiCupoCompletoActividad(ID_REQUERIMIENTO_ACT);

        assertThat(rechazadas).isZero();
        verify(postulacionActividadRepository, never())
                .findPendientesPorRequerimientoParaActualizar(any());
        verify(invitacionActividadRepository, never())
                .findPendientesPorRequerimientoParaActualizar(any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void general_siLaNotificacionYaExiste_noLaDuplica() {
        RequerimientoGralProyecto requerimiento = requerimientoGeneral();
        PostulacionGral postulacion = postulacionGeneral(31L, 3L);

        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));
        when(postulacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(ID_REQUERIMIENTO_GRAL))
                .thenReturn(List.of(postulacion));
        when(invitacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(ID_REQUERIMIENTO_GRAL))
                .thenReturn(List.of());
        when(notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadId(
                        any(), anyString(), anyString(), any())).thenReturn(true);

        int rechazadas = servicio.rechazarPendientesSiCupoCompletoGeneral(ID_REQUERIMIENTO_GRAL);

        assertThat(rechazadas).isEqualTo(1);
        assertThat(postulacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());

        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void general_cupoCompleto_rechazaPostulacionEInvitacionYNotifica() {
        RequerimientoGralProyecto requerimiento = requerimientoGeneral();
        PostulacionGral postulacion = postulacionGeneral(41L, 4L);
        InvitacionGral invitacion = invitacionGeneral(51L, 5L);

        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));
        when(postulacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(ID_REQUERIMIENTO_GRAL))
                .thenReturn(List.of(postulacion));
        when(invitacionGralRepository
                .findPendientesPorRequerimientoGralParaActualizar(ID_REQUERIMIENTO_GRAL))
                .thenReturn(List.of(invitacion));
        when(notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadId(
                        any(), anyString(), anyString(), any())).thenReturn(false);

        int rechazadas = servicio.rechazarPendientesSiCupoCompletoGeneral(ID_REQUERIMIENTO_GRAL);

        assertThat(rechazadas).isEqualTo(2);
        assertThat(postulacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(invitacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(invitacion.getFechaRechazo()).isNotNull();

        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    private RequerimientoActividad requerimientoActividad() {
        Proyecto proyecto = proyecto();
        Actividad actividad = Actividad.builder()
                .idActividad(60L)
                .nombre("Sesión de fotos")
                .planificacion(Planificacion.builder()
                        .idPlanificacion(70L)
                        .proyecto(proyecto)
                        .build())
                .build();

        return RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO_ACT)
                .cantidad(1)
                .actividad(actividad)
                .build();
    }

    private RequerimientoGralProyecto requerimientoGeneral() {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(ID_REQUERIMIENTO_GRAL)
                .cantidad(1)
                .proyecto(proyecto())
                .build();
    }

    private Proyecto proyecto() {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto Modalink")
                .build();
    }

    private PostulacionActividad postulacionActividad(Long id, Long idPerfil) {
        return PostulacionActividad.builder()
                .idPostulacion(id)
                .estado(EstadoSolicitud.PENDIENTE)
                .perfil(perfil(idPerfil))
                .requerimiento(requerimientoActividad())
                .build();
    }

    private InvitacionActividad invitacionActividad(Long id, Long idPerfil) {
        return InvitacionActividad.builder()
                .idInvitacion(id)
                .estado(EstadoSolicitud.PENDIENTE)
                .perfilDestinatario(perfil(idPerfil))
                .requerimiento(requerimientoActividad())
                .build();
    }

    private PostulacionGral postulacionGeneral(Long id, Long idPerfil) {
        return PostulacionGral.builder()
                .idPostulacionGral(id)
                .estado(EstadoSolicitud.PENDIENTE)
                .perfil(perfil(idPerfil))
                .requerimientoGral(requerimientoGeneral())
                .build();
    }

    private InvitacionGral invitacionGeneral(Long id, Long idPerfil) {
        return InvitacionGral.builder()
                .idInvitacionGral(id)
                .estado(EstadoSolicitud.PENDIENTE)
                .perfilDestinatario(perfil(idPerfil))
                .proyecto(proyecto())
                .requerimientoGral(requerimientoGeneral())
                .build();
    }

    private Perfil perfil(Long id) {
        return Perfil.builder()
                .idPerfil(id)
                .usuario(Usuario.builder().idUsuario(id + 100).build())
                .build();
    }
}
