package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionarSolicitudConexionServiceTest {

    private static final Long ID_USUARIO_REMITENTE = 1L;
    private static final Long ID_USUARIO_DESTINATARIO = 2L;
    private static final Long ID_PERFIL_REMITENTE = 10L;
    private static final Long ID_PERFIL_DESTINATARIO = 20L;
    private static final Long ID_SOLICITUD = 100L;
    private static final Long ID_CONEXION = 55L;

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private SolicitudConexionRepository solicitudConexionRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private NotificacionService notificacionService;

    private GestionarSolicitudConexionService service;

    private Perfil perfilRemitente;
    private Perfil perfilDestinatario;
    private SolicitudConexion solicitud;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-09T13:30:00Z"), ZoneOffset.UTC);

        service = new GestionarSolicitudConexionService(
                perfilActivoService,
                solicitudConexionRepository,
                conexionRepository,
                notificacionService,
                reloj);

        perfilRemitente = perfil(
                ID_PERFIL_REMITENTE, "Ana Moda", "Ana", "Pérez", 1L, "Modelo");
        perfilDestinatario = perfil(
                ID_PERFIL_DESTINATARIO, "Bruno Estilo", "Bruno", "Gómez", 2L, "Fotógrafo");

        solicitud = SolicitudConexion.builder()
                .idSolicitudConexion(ID_SOLICITUD)
                .perfilRemitente(perfilRemitente)
                .perfilDestinatario(perfilDestinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 8, 10, 0))
                .build();

        when(perfilActivoService.resolver(ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO))
                .thenReturn(perfilDestinatario);
        when(solicitudConexionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.of(solicitud));
        when(conexionRepository.existsActivaEntre(
                ID_PERFIL_REMITENTE, ID_PERFIL_DESTINATARIO))
                .thenReturn(false);
        when(solicitudConexionRepository.saveAndFlush(any(SolicitudConexion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(conexionRepository.saveAndFlush(any(Conexion.class)))
                .thenAnswer(invocacion -> {
                    Conexion conexion = invocacion.getArgument(0);
                    conexion.setIdConexion(ID_CONEXION);
                    return conexion;
                });
        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Notificacion());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void aceptar_creaConexionYNotificaAAmbasPartes() {
        SolicitudConexionGestionadaResponse respuesta = service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idConexion()).isEqualTo(ID_CONEXION);
        assertThat(respuesta.fechaRespuesta())
                .isEqualTo(LocalDateTime.of(2026, 11, 9, 13, 30));
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);

        ArgumentCaptor<Conexion> conexionCaptor = ArgumentCaptor.forClass(Conexion.class);
        verify(conexionRepository).saveAndFlush(conexionCaptor.capture());
        Conexion conexion = conexionCaptor.getValue();
        assertThat(conexion.getPerfil1().getIdPerfil()).isEqualTo(ID_PERFIL_REMITENTE);
        assertThat(conexion.getPerfil2().getIdPerfil()).isEqualTo(ID_PERFIL_DESTINATARIO);
        assertThat(conexion.getSolicitudOrigen()).isEqualTo(solicitud);
        assertThat(conexion.getEstado()).isEqualTo(EstadoConexion.ACTIVA);
        assertThat(conexion.getFechaInicioConexion())
                .isEqualTo(LocalDateTime.of(2026, 11, 9, 13, 30));

        verify(notificacionService, times(2)).crear(
                any(), any(),
                eq(NotificacionService.TIPO_SOLICITUD_CONEXION_ACEPTADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_CONEXION),
                eq(ID_CONEXION));

        InOrder orden = inOrder(solicitudConexionRepository, conexionRepository);
        orden.verify(solicitudConexionRepository)
                .saveAndFlush(any(SolicitudConexion.class));
        orden.verify(conexionRepository).saveAndFlush(any(Conexion.class));
    }

    @Test
    void rechazar_soloNotificaAlRemitente() {
        SolicitudConexionGestionadaResponse respuesta = service.rechazar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idConexion()).isNull();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);

        verify(conexionRepository, never()).saveAndFlush(any(Conexion.class));
        verify(notificacionService, times(1)).crear(
                eq(perfilRemitente.getUsuario()),
                eq(perfilRemitente),
                eq(NotificacionService.TIPO_SOLICITUD_CONEXION_RECHAZADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_SOLICITUD_CONEXION),
                eq(ID_SOLICITUD));
    }

    @Test
    void aceptar_solicitudDeOtroPerfil_lanzaNoPermitida() {
        when(perfilActivoService.resolver(ID_USUARIO_REMITENTE, ID_PERFIL_REMITENTE))
                .thenReturn(perfilRemitente);

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_REMITENTE, ID_PERFIL_REMITENTE, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoPermitidaException.class);
    }

    @Test
    void aceptar_solicitudInexistente_lanzaNoEncontrada() {
        when(solicitudConexionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoEncontradaException.class);
    }

    @Test
    void aceptar_solicitudNoPendiente_lanzaNoPendiente() {
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoPendienteException.class);
    }

    @Test
    void aceptar_conexionExistente_lanzaConexionYaExistente() {
        when(conexionRepository.existsActivaEntre(
                ID_PERFIL_REMITENTE, ID_PERFIL_DESTINATARIO))
                .thenReturn(true);

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(ConexionYaExistenteException.class);
    }

    @Test
    void aceptar_remitenteInactivo_lanzaPerfilEnBaja() {
        perfilRemitente.setEstado(EstadoPerfil.PendienteBaja);

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void aceptar_destinatarioInactivo_lanzaPerfilEnBaja() {
        perfilDestinatario.setEstado(EstadoPerfil.PendienteBaja);

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void aceptar_violacionDeIndiceUnico_lanzaConexionYaExistente() {
        when(conexionRepository.saveAndFlush(any(Conexion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicado",
                        new RuntimeException("UQ_conexion_activa")));

        assertThatThrownBy(() -> service.aceptar(
                ID_USUARIO_DESTINATARIO, ID_PERFIL_DESTINATARIO, ID_SOLICITUD))
                .isInstanceOf(ConexionYaExistenteException.class);
    }

    private Perfil perfil(
            Long idPerfil,
            String nombreArtistico,
            String nombre,
            String apellido,
            Long idProfesion,
            String nombreProfesion) {

        Usuario usuario = Usuario.builder()
                .idUsuario(idPerfil.equals(ID_PERFIL_REMITENTE)
                        ? ID_USUARIO_REMITENTE
                        : ID_USUARIO_DESTINATARIO)
                .nombre(nombre)
                .apellido(apellido)
                .estado(EstadoUsuario.Activo)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(org.mgroko.backend.modelo.Profesion.builder()
                        .idProfesion(idProfesion)
                        .codigo("PROF_" + idProfesion)
                        .nombre(nombreProfesion)
                        .build())
                .build();
    }
}
