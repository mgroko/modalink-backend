package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.LinkedHashSet;
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
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionarSolicitudColaboracionServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-10T23:30:00Z"), ZoneOffset.UTC);

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private NotificacionService notificacionService;

    private GestionarSolicitudColaboracionService servicio;
    private Usuario usuarioRemitente;
    private Usuario usuarioDestinatario;
    private Perfil remitente;
    private Perfil destinatario;
    private Publicacion publicacion;
    private SolicitudColaboracion solicitud;

    @BeforeEach
    void setUp() {
        servicio = new GestionarSolicitudColaboracionService(
                perfilActivoService,
                solicitudColaboracionRepository,
                publicacionRepository,
                notificacionService,
                RELOJ);

        usuarioRemitente = Usuario.builder()
                .idUsuario(2L)
                .estado(EstadoUsuario.Activo)
                .build();
        usuarioDestinatario = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();

        remitente = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioRemitente)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .imagen(Imagen.builder().idImagen(1L).url("/uploads/luna.webp").build())
                .build();
        destinatario = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(remitente)
                .colaboradores(new LinkedHashSet<>())
                .build();

        solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(50L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 0))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .build();

        when(perfilActivoService.resolver(1L, 7L)).thenReturn(destinatario);
        when(solicitudColaboracionRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(solicitud));
        when(solicitudColaboracionRepository.saveAndFlush(any(SolicitudColaboracion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(publicacionRepository.saveAndFlush(any(Publicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(false);
        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());
    }

    @Test
    void aceptar_agregaColaboradorActualizaEstadoYNotificaAlEmisor() {
        SolicitudColaboracionGestionadaResponse response = servicio.aceptar(1L, 7L, 50L);

        assertEquals(EstadoSolicitud.ACEPTADA, response.estado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 23, 30), response.fechaRespuesta());
        assertTrue(response.colaboradorAgregado());
        assertEquals(EstadoSolicitud.ACEPTADA, solicitud.getEstado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 23, 30), solicitud.getFechaRespuesta());
        assertTrue(publicacion.getColaboradores().contains(destinatario));
        verify(publicacionRepository).saveAndFlush(publicacion);
        verify(notificacionService).crear(
                eq(usuarioRemitente),
                eq(remitente),
                eq(NotificacionService.TIPO_SOLICITUD_COLABORACION_ACEPTADA),
                eq("El perfil 'Sol' aceptó tu solicitud para colaborar en la publicación 'Editorial Otoño'."),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void rechazar_actualizaEstadoYNotificaSinAgregarColaborador() {
        SolicitudColaboracionGestionadaResponse response = servicio.rechazar(1L, 7L, 50L);

        assertEquals(EstadoSolicitud.RECHAZADA, response.estado());
        assertEquals(LocalDateTime.of(2026, 11, 10, 23, 30), response.fechaRespuesta());
        assertFalse(response.colaboradorAgregado());
        assertEquals(EstadoSolicitud.RECHAZADA, solicitud.getEstado());
        assertTrue(publicacion.getColaboradores().isEmpty());
        verify(publicacionRepository, never()).saveAndFlush(any(Publicacion.class));
        verify(notificacionService).crear(
                eq(usuarioRemitente),
                eq(remitente),
                eq(NotificacionService.TIPO_SOLICITUD_COLABORACION_RECHAZADA),
                eq("El perfil 'Sol' rechazó tu solicitud para colaborar en la publicación 'Editorial Otoño'."),
                eq(NotificacionService.ENTIDAD_PUBLICACION),
                eq(99L));
    }

    @Test
    void aceptar_solicitudInexistente_lanzaNoEncontrada() {
        when(solicitudColaboracionRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.empty());

        assertThrows(SolicitudColaboracionNoEncontradaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));
    }

    @Test
    void aceptar_perfilActivoNoEsDestinatario_lanzaNoPermitida() {
        Perfil otro = Perfil.builder()
                .idPerfil(9L)
                .nombreArtistico("Otro")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(Profesion.builder().idProfesion(5L).nombre("Modelo").build())
                .build();
        when(perfilActivoService.resolver(1L, 7L)).thenReturn(otro);

        assertThrows(SolicitudColaboracionNoPermitidaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));
        verify(solicitudColaboracionRepository, never())
                .saveAndFlush(any(SolicitudColaboracion.class));
    }

    @Test
    void aceptar_solicitudNoPendiente_lanzaNoPendiente() {
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        assertThrows(SolicitudColaboracionNoPendienteException.class,
                () -> servicio.aceptar(1L, 7L, 50L));
    }

    @Test
    void aceptar_publicacionOculta_lanzaNoDisponible() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));

        assertEquals("PUBLICACION_NO_DISPONIBLE", ex.getCodigo());
        verify(publicacionRepository, never()).saveAndFlush(any(Publicacion.class));
    }

    @Test
    void aceptar_remitenteEnBaja_lanzaNoDisponible() {
        remitente.setEstado(EstadoPerfil.Baja);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));

        assertEquals("PUBLICACION_NO_DISPONIBLE", ex.getCodigo());
    }

    @Test
    void aceptar_yaEsColaborador_lanzaConflicto() {
        when(publicacionRepository.existsColaborador(99L, 8L)).thenReturn(true);

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));

        assertEquals("DESTINATARIO_YA_ES_COLABORADOR", ex.getCodigo());
        verify(publicacionRepository, never()).saveAndFlush(any(Publicacion.class));
    }

    @Test
    void aceptar_duplicadoPorIndice_lanzaConflicto() {
        when(publicacionRepository.saveAndFlush(any(Publicacion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicado",
                        new SQLException("PK_colaborador_publicacion")));

        SolicitudColaboracionInvalidaException ex = assertThrows(
                SolicitudColaboracionInvalidaException.class,
                () -> servicio.aceptar(1L, 7L, 50L));

        assertEquals("DESTINATARIO_YA_ES_COLABORADOR", ex.getCodigo());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void rechazar_publicacionOculta_permiteCerrarLaSolicitud() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);

        SolicitudColaboracionGestionadaResponse response = servicio.rechazar(1L, 7L, 50L);

        assertEquals(EstadoSolicitud.RECHAZADA, response.estado());
        assertTrue(publicacion.getColaboradores().isEmpty());
    }
}
