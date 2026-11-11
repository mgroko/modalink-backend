package org.mgroko.backend.publicaciones.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarSolicitudColaboracionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 7L;
    private static final Long ID_SOLICITUD = 50L;
    private static final Long ID_PUBLICACION = 99L;

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private SolicitudColaboracionRepository solicitudColaboracionRepository;

    @Mock
    private NotificacionService notificacionService;

    private EliminarSolicitudColaboracionService servicio;
    private Usuario usuarioRemitente;
    private Usuario usuarioDestinatario;
    private Perfil remitente;
    private Perfil destinatario;
    private Publicacion publicacion;
    private SolicitudColaboracion solicitud;

    @BeforeEach
    void setUp() {
        servicio = new EliminarSolicitudColaboracionService(
                perfilActivoService,
                solicitudColaboracionRepository,
                notificacionService);

        usuarioRemitente = Usuario.builder()
                .idUsuario(ID_USUARIO)
                .estado(EstadoUsuario.Activo)
                .build();
        usuarioDestinatario = Usuario.builder()
                .idUsuario(2L)
                .estado(EstadoUsuario.Activo)
                .build();

        remitente = Perfil.builder()
                .idPerfil(ID_PERFIL_ACTIVO)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioRemitente)
                .profesion(Profesion.builder()
                        .idProfesion(3L)
                        .nombre("Diseñadora")
                        .build())
                .build();
        destinatario = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(Profesion.builder()
                        .idProfesion(4L)
                        .nombre("Fotógrafa")
                        .build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(ID_PUBLICACION)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(remitente)
                .build();

        solicitud = SolicitudColaboracion.builder()
                .idSolicitudColaboracion(ID_SOLICITUD)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.of(2026, 11, 10, 20, 0))
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .build();

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(remitente);
        when(solicitudColaboracionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.of(solicitud));
        when(notificacionService.eliminarPorEntidadYPerfil(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                NotificacionService.ENTIDAD_PUBLICACION,
                ID_PUBLICACION,
                usuarioDestinatario.getIdUsuario(),
                destinatario.getIdPerfil()))
                .thenReturn(1);
    }

    @Test
    void eliminar_solicitudPropiaPendiente_eliminaSolicitudYNotificacion() {
        servicio.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD);

        verify(notificacionService).eliminarPorEntidadYPerfil(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                NotificacionService.ENTIDAD_PUBLICACION,
                ID_PUBLICACION,
                usuarioDestinatario.getIdUsuario(),
                destinatario.getIdPerfil());
        verify(solicitudColaboracionRepository).delete(solicitud);
        verify(solicitudColaboracionRepository).flush();
    }

    @Test
    void eliminar_publicacionOculta_eliminaIgual() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);

        servicio.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD);

        verify(solicitudColaboracionRepository).delete(solicitud);
        verify(solicitudColaboracionRepository).flush();
    }

    @Test
    void eliminar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        when(perfilActivoService.resolver(ID_USUARIO, null))
                .thenThrow(new PerfilActivoNoSeleccionadoException());

        assertThatThrownBy(() -> servicio.eliminar(ID_USUARIO, null, ID_SOLICITUD))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }

    @Test
    void eliminar_solicitudInexistente_lanzaNoEncontrada() {
        when(solicitudColaboracionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudColaboracionNoEncontradaException.class);

        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }

    @Test
    void eliminar_solicitudDeOtroPerfil_lanzaNoPermitida() {
        solicitud.setPerfilRemitente(destinatario);

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudColaboracionNoPermitidaException.class);

        verify(notificacionService, never()).eliminarPorEntidadYPerfil(
                any(), any(), any(), any(), any());
        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }

    @Test
    void eliminar_solicitudAceptada_lanzaNoPendiente() {
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudColaboracionNoPendienteException.class);

        verify(notificacionService, never()).eliminarPorEntidadYPerfil(
                any(), any(), any(), any(), any());
        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }

    @Test
    void eliminar_solicitudRechazada_lanzaNoPendiente() {
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudColaboracionNoPendienteException.class);

        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }

    @Test
    void eliminar_perfilActivoEnBaja_lanzaPerfilEnBaja() {
        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenThrow(new PerfilEnBajaException("El perfil no se encuentra activo."));

        assertThatThrownBy(() -> servicio.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(PerfilEnBajaException.class);

        verify(solicitudColaboracionRepository, never())
                .delete(any(SolicitudColaboracion.class));
    }
}
