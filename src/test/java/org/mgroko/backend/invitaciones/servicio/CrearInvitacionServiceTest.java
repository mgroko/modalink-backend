package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
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
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearInvitacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_REMITENTE = 10L;
    private static final Long ID_PERFIL_DESTINATARIO = 20L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_PROFESION = 3L;
    private static final Long ID_REQUERIMIENTO = 30L;
    private static final Long ID_ACTIVIDAD = 40L;

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 12, 0);

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private NotificacionService notificacionService;

    private CrearInvitacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CrearInvitacionService(
                perfilRepository,
                proyectoRepository,
                miembroProyectoRepository,
                requerimientoActividadRepository,
                requerimientoGralProyectoRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                cupoRequerimientoService,
                proyectoSecurityService,
                notificacionService,
                RELOJ);
    }

    @Test
    void general_conDatosValidos_persisteYNotifica() {
        prepararGeneral();

        when(invitacionGralRepository.saveAndFlush(any(InvitacionGral.class)))
                .thenAnswer(invocacion -> {
                    InvitacionGral invitacion = invocacion.getArgument(0);
                    invitacion.setIdInvitacionGral(77L);
                    return invitacion;
                });

        InvitacionResponse respuesta = servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, "  Hola  "));

        assertThat(respuesta.idInvitacion()).isEqualTo(77L);
        assertThat(respuesta.tipo()).isEqualTo(TipoInvitacion.GENERAL);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(respuesta.mensaje()).isEqualTo("Hola");
        assertThat(respuesta.idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(respuesta.idRequerimientoActividad()).isNull();

        ArgumentCaptor<InvitacionGral> captor = ArgumentCaptor.forClass(InvitacionGral.class);
        verify(invitacionGralRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFechaEnvio()).isEqualTo(AHORA);
        assertThat(captor.getValue().getProyecto().getIdProyecto()).isEqualTo(ID_PROYECTO);

        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void general_conMensajeVacio_guardaNull() {
        prepararGeneral();
        when(invitacionGralRepository.saveAndFlush(any(InvitacionGral.class)))
                .thenAnswer(invocacion -> {
                    InvitacionGral invitacion = invocacion.getArgument(0);
                    invitacion.setIdInvitacionGral(77L);
                    return invitacion;
                });

        InvitacionResponse respuesta = servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, "   "));

        assertThat(respuesta.mensaje()).isNull();
    }

    @Test
    void general_conInvitacionPendienteDelProyecto_lanzaConflicto() {
        prepararGeneral();
        when(invitacionGralRepository
                .existsByProyecto_IdProyectoAndPerfilDestinatario_IdPerfilAndEstado(
                        ID_PROYECTO, ID_PERFIL_DESTINATARIO, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("invitación activa");

        verify(invitacionGralRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void general_conInvitacionDeActividadPendiente_lanzaConflicto() {
        prepararGeneral();
        when(invitacionActividadRepository.existsDeProyectoYDestinatarioEnEstado(
                ID_PROYECTO, ID_PERFIL_DESTINATARIO, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("invitación activa");
    }

    @Test
    void general_conDestinatarioMiembroActivo_lanzaConflicto() {
        prepararGeneral();
        when(miembroProyectoRepository.findMiembroActivoConRol(
                ID_PROYECTO, ID_PERFIL_DESTINATARIO))
                .thenReturn(Optional.of(MiembroProyecto.builder()
                        .idMiembro(50L)
                        .rolProyecto(RolProyecto.builder().idRolProyecto(2L).build())
                        .build()));

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("miembro activo");
    }

    @Test
    void general_conAutoinvitacion_lanzaConflicto() {
        prepararGeneral();
        when(perfilRepository.findByIdConProfesion(ID_PERFIL_REMITENTE))
                .thenReturn(Optional.of(perfil(ID_PERFIL_REMITENTE, EstadoPerfil.ACTIVO,
                        ID_PROFESION, EstadoUsuario.ACTIVO)));

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_REMITENTE, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("vos mismo");
    }

    @Test
    void general_conProyectoEnBorrador_lanzaConflicto() {
        prepararGeneral();
        when(proyectoRepository.findByIdParaGestion(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto(EstadoProyecto.BORRADOR)));

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("Publicados o Confirmados");
    }

    @Test
    void actividad_conDatosValidos_persisteYNotifica() {
        prepararActividad(EstadoActividad.PENDIENTE, ID_PROFESION, ID_PROYECTO);

        when(invitacionActividadRepository.saveAndFlush(any(InvitacionActividad.class)))
                .thenAnswer(invocacion -> {
                    InvitacionActividad invitacion = invocacion.getArgument(0);
                    invitacion.setIdInvitacion(88L);
                    return invitacion;
                });

        InvitacionResponse respuesta = servicio.invitarAProyectoActividad(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionActividadRequest(
                        ID_REQUERIMIENTO, ID_PERFIL_DESTINATARIO, "Mensaje"));

        assertThat(respuesta.idInvitacion()).isEqualTo(88L);
        assertThat(respuesta.tipo()).isEqualTo(TipoInvitacion.ACTIVIDAD);
        assertThat(respuesta.idRequerimientoActividad()).isEqualTo(ID_REQUERIMIENTO);
        assertThat(respuesta.idProyecto()).isEqualTo(ID_PROYECTO);

        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void actividad_enEstadoFinalizada_lanzaConflicto() {
        prepararActividad(EstadoActividad.FINALIZADA, ID_PROFESION, ID_PROYECTO);

        assertThatThrownBy(() -> servicio.invitarAProyectoActividad(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionActividadRequest(
                        ID_REQUERIMIENTO, ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("Pendientes o En curso");
    }

    @Test
    void actividad_conProfesionDistinta_lanzaConflicto() {
        prepararActividad(EstadoActividad.PENDIENTE, 99L, ID_PROYECTO);

        assertThatThrownBy(() -> servicio.invitarAProyectoActividad(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionActividadRequest(
                        ID_REQUERIMIENTO, ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("profesión requerida");
    }

    @Test
    void actividad_deOtroProyecto_lanzaNoEncontrado() {
        prepararActividad(EstadoActividad.PENDIENTE, ID_PROFESION, 99L);

        assertThatThrownBy(() -> servicio.invitarAProyectoActividad(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionActividadRequest(
                        ID_REQUERIMIENTO, ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(RequerimientoNoEncontradoException.class);
    }

    @Test
    void general_conDestinatarioInactivo_lanzaConflicto() {
        prepararGeneral();
        Perfil destinatario = perfil(ID_PERFIL_DESTINATARIO, EstadoPerfil.BAJA,
                ID_PROFESION, EstadoUsuario.ACTIVO);
        when(perfilRepository.findByIdConProfesion(ID_PERFIL_DESTINATARIO))
                .thenReturn(Optional.of(destinatario));

        assertThatThrownBy(() -> servicio.invitarAProyecto(
                ID_USUARIO, ID_PERFIL_REMITENTE, ID_PROYECTO,
                new CrearInvitacionGralRequest(ID_PERFIL_DESTINATARIO, null)))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    private void prepararGeneral() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(
                ID_PERFIL_REMITENTE, ID_USUARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL_REMITENTE, EstadoPerfil.ACTIVO,
                        ID_PROFESION, EstadoUsuario.ACTIVO)));
        when(proyectoRepository.findByIdParaGestion(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto(EstadoProyecto.PUBLICADO)));
        when(perfilRepository.findByIdConProfesion(ID_PERFIL_DESTINATARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL_DESTINATARIO, EstadoPerfil.ACTIVO,
                        ID_PROFESION, EstadoUsuario.ACTIVO)));
        when(miembroProyectoRepository.findMiembroActivoConRol(
                ID_PROYECTO, ID_PERFIL_DESTINATARIO))
                .thenReturn(Optional.empty());
        when(invitacionGralRepository
                .existsByProyecto_IdProyectoAndPerfilDestinatario_IdPerfilAndEstado(
                        ID_PROYECTO, ID_PERFIL_DESTINATARIO, EstadoSolicitud.PENDIENTE))
                .thenReturn(false);
        when(invitacionActividadRepository.existsDeProyectoYDestinatarioEnEstado(
                ID_PROYECTO, ID_PERFIL_DESTINATARIO, EstadoSolicitud.PENDIENTE))
                .thenReturn(false);
        prepararNotificacion();
    }

    private void prepararActividad(
            EstadoActividad estadoActividad,
            Long idProfesionRequerimiento,
            Long idProyectoRequerimiento) {

        prepararGeneral();
        when(requerimientoActividadRepository.findByIdParaGestion(ID_REQUERIMIENTO))
                .thenReturn(Optional.of(requerimiento(
                        estadoActividad, idProfesionRequerimiento, idProyectoRequerimiento)));
        when(cupoRequerimientoService.calcularActividad(any()))
                .thenReturn(new CupoRequerimiento(2, 0, 2, false));
    }

    private void prepararNotificacion() {
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
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Perfil perfil(
            Long idPerfil,
            EstadoPerfil estado,
            Long idProfesion,
            EstadoUsuario estadoUsuario) {

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico("Perfil " + idPerfil)
                .estado(estado)
                .usuario(Usuario.builder()
                        .idUsuario(idPerfil + 1000)
                        .estado(estadoUsuario)
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(idProfesion)
                        .nombre("Profesión " + idProfesion)
                        .build())
                .build();
    }

    private Proyecto proyecto(EstadoProyecto estado) {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto UC31")
                .descripcion("Descripción")
                .estado(estado)
                .build();
    }

    private RequerimientoActividad requerimiento(
            EstadoActividad estadoActividad,
            Long idProfesionRequerimiento,
            Long idProyectoRequerimiento) {

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(idProyectoRequerimiento)
                .nombre("Proyecto UC31")
                .build();
        Actividad actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Actividad UC31")
                .estado(estadoActividad)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(60L)
                        .proyecto(proyecto)
                        .build())
                .build();

        return RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(2)
                .descripcion("Requerimiento UC31")
                .profesion(Profesion.builder()
                        .idProfesion(idProfesionRequerimiento)
                        .nombre("Profesión requerida")
                        .build())
                .actividad(actividad)
                .build();
    }
}
