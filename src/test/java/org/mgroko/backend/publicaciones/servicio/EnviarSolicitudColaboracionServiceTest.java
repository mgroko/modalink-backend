package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionDuplicadaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnviarSolicitudColaboracionServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-10T23:30:00Z"), ZoneOffset.UTC);

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private NotificacionService notificacionService;

    private EnviarSolicitudColaboracionService servicio;
    private Usuario usuarioRemitente;
    private Usuario usuarioDestinatario;
    private Perfil remitente;
    private Perfil destinatario;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new EnviarSolicitudColaboracionService(
                perfilActivoService,
                perfilRepository,
                publicacionRepository,
                solicitudColaboracionRepository,
                conexionRepository,
                notificacionService,
                RELOJ);

        usuarioRemitente = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();
        usuarioDestinatario = Usuario.builder()
                .idUsuario(2L)
                .estado(EstadoUsuario.Activo)
                .build();

        remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioRemitente)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .imagen(Imagen.builder().idImagen(1L).url("/uploads/perfiles/luna.webp").build())
                .build();

        destinatario = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .imagen(Imagen.builder().idImagen(2L).url("/uploads/perfiles/sol.webp").build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(remitente)
                .build();

        when(perfilActivoService.resolver(1L, 7L)).thenReturn(remitente);
        when(perfilRepository.findById(8L)).thenReturn(Optional.of(destinatario));
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.of(publicacion));
        when(conexionRepository.existsActivaEntre(7L, 8L)).thenReturn(true);
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(false);
        when(solicitudColaboracionRepository
                .existsByPublicacionIdPublicacionAndPerfilDestinatarioIdPerfilAndEstado(
                        99L, 8L, EstadoSolicitud.PENDIENTE))
                .thenReturn(false);
        when(solicitudColaboracionRepository.saveAndFlush(any(SolicitudColaboracion.class)))
                .thenAnswer(invocation -> {
                    SolicitudColaboracion solicitud = invocation.getArgument(0);
                    solicitud.setIdSolicitudColaboracion(50L);
                    return solicitud;
                });
        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());
    }

    private EnviarSolicitudColaboracionRequest request(Long idDestinatario) {
        return new EnviarSolicitudColaboracionRequest(idDestinatario);
    }

    @Test
    void enviar_creaSolicitudPendienteYNotificaAlDestinatario() {
        SolicitudColaboracionResponse response = servicio.enviar(1L, 7L, 99L, request(8L));

        assertEquals(50L, response.idSolicitudColaboracion());
        assertEquals(EstadoSolicitud.PENDIENTE, response.estado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 23, 30), response.fechaEnvio());
        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial Otoño", response.tituloPublicacion());
        assertEquals(7L, response.remitente().idPerfil());
        assertEquals(8L, response.destinatario().idPerfil());

        ArgumentCaptor<SolicitudColaboracion> captor =
                ArgumentCaptor.forClass(SolicitudColaboracion.class);
        verify(solicitudColaboracionRepository).saveAndFlush(captor.capture());
        assertEquals(EstadoSolicitud.PENDIENTE, captor.getValue().getEstado());
        assertEquals(publicacion, captor.getValue().getPublicacion());
        assertEquals(remitente, captor.getValue().getPerfilRemitente());
        assertEquals(destinatario, captor.getValue().getPerfilDestinatario());

        verify(notificacionService).crear(
                eq(usuarioDestinatario),
                eq(destinatario),
                eq(NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA),
                eq("El perfil 'Luna Diseños' te invitó a colaborar en la publicación 'Editorial Otoño'."),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void enviar_sinPerfilActivo_propagaExcepcion() {
        when(perfilActivoService.resolver(1L, null))
                .thenThrow(new org.mgroko.backend.perfiles.exception
                        .PerfilActivoNoSeleccionadoException());

        assertThrows(org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException.class,
                () -> servicio.enviar(1L, null, 99L, request(8L)));
    }

    @Test
    void enviar_publicacionInexistenteOAjena_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
    }

    @Test
    void enviar_publicacionOculta_lanzaNoDisponible() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));

        assertEquals("PUBLICACION_NO_DISPONIBLE", ex.getCodigo());
    }

    @Test
    void enviar_destinatarioInexistente_lanzaPerfilNoEncontrado() {
        when(perfilRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
    }

    @Test
    void enviar_destinatarioEnBaja_lanzaPerfilNoEncontrado() {
        destinatario.setEstado(EstadoPerfil.Baja);

        assertThrows(PerfilNoEncontradoException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
    }

    @Test
    void enviar_usuarioDestinatarioInactivo_lanzaPerfilNoEncontrado() {
        usuarioDestinatario.setEstado(EstadoUsuario.Baja);

        assertThrows(PerfilNoEncontradoException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
    }

    @Test
    void enviar_alMismoPerfil_lanzaAutoinvitacion() {
        destinatario.setIdPerfil(7L);
        destinatario.setUsuario(usuarioRemitente);
        when(perfilRepository.findById(7L)).thenReturn(Optional.of(destinatario));

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(7L)));

        assertEquals("AUTOINVITACION", ex.getCodigo());
    }

    @Test
    void enviar_otroPerfilDelMismoUsuario_creaSolicitudSinConexion() {
        destinatario.setUsuario(usuarioRemitente);
        when(conexionRepository.existsActivaEntre(7L, 8L)).thenReturn(false);

        SolicitudColaboracionResponse response = servicio.enviar(
                1L, 7L, 99L, request(8L));

        assertEquals(50L, response.idSolicitudColaboracion());
        assertEquals(EstadoSolicitud.PENDIENTE, response.estado());
        verify(solicitudColaboracionRepository)
                .saveAndFlush(any(SolicitudColaboracion.class));
        verify(conexionRepository, never()).existsActivaEntre(7L, 8L);
        verify(notificacionService).crear(
                eq(usuarioRemitente),
                eq(destinatario),
                eq(NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA),
                any(),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
    }

    @Test
    void enviar_fueraDeLaRed_lanzaDestinatarioFueraDeRed() {
        when(conexionRepository.existsActivaEntre(7L, 8L)).thenReturn(false);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));

        assertEquals("DESTINATARIO_FUERA_DE_RED", ex.getCodigo());
        verify(solicitudColaboracionRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviar_destinatarioYaEsColaborador_lanzaInvalida() {
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(true);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));

        assertEquals("DESTINATARIO_YA_ES_COLABORADOR", ex.getCodigo());
        verify(solicitudColaboracionRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviar_solicitudPendienteDuplicada_lanzaDuplicada() {
        when(solicitudColaboracionRepository
                .existsByPublicacionIdPublicacionAndPerfilDestinatarioIdPerfilAndEstado(
                        99L, 8L, EstadoSolicitud.PENDIENTE))
                .thenReturn(true);

        assertThrows(SolicitudColaboracionDuplicadaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
        verify(solicitudColaboracionRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void enviar_duplicadaPorIndiceUnico_lanzaDuplicadaYNoNotifica() {
        when(solicitudColaboracionRepository.saveAndFlush(any(SolicitudColaboracion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicada",
                        new SQLException("UQ_solicitud_colaboracion_pendiente")));

        assertThrows(SolicitudColaboracionDuplicadaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void enviar_errorDeIntegridadGenerico_lanzaInvalidaYNoNotifica() {
        when(solicitudColaboracionRepository.saveAndFlush(any(SolicitudColaboracion.class)))
                .thenThrow(new DataIntegrityViolationException("otro"));

        assertThrows(SolicitudColaboracionInvalidaException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(8L)));
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void enviar_requestSinDestinatario_lanzaPerfilNoEncontradoAntesDeGuardar() {
        assertThrows(PerfilNoEncontradoException.class,
                () -> servicio.enviar(1L, 7L, 99L, request(null)));
        verify(solicitudColaboracionRepository, never()).saveAndFlush(any());
    }
}
