package org.mgroko.backend.notificaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;
import org.mgroko.backend.reportes.evento.ReportePublicacionCreadoEvent;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportePublicacionNotificacionListenerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionService notificacionService;

    private final ReportePublicacionCreadoEvent evento = new ReportePublicacionCreadoEvent(
            55L,
            30L,
            "Publicación de prueba",
            20L,
            "Perfil Dueño",
            1L,
            "Luna Pérez",
            10L,
            "Luna Diseños",
            MotivoReportePublicacion.CONTENIDO_INAPROPIADO,
            "Contenido ofensivo");

    @Test
    void alCrearReporte_notificaATodosLosAdminsActivos() {
        Usuario adminUno = admin(90L);
        Usuario adminDos = admin(91L);
        when(usuarioRepository.findAdministradoresActivos())
                .thenReturn(List.of(adminUno, adminDos));
        when(notificacionService.crear(
                any(Usuario.class), isNull(), anyString(), anyString(), anyString(), any(), isNull()))
                .thenReturn(Notificacion.builder().build());

        new ReportePublicacionNotificacionListener(usuarioRepository, notificacionService)
                .alCrearReporte(evento);

        verify(notificacionService, times(2)).crear(
                any(Usuario.class),
                isNull(),
                eq(NotificacionService.TIPO_PUBLICACION_REPORTADA),
                anyString(),
                eq(NotificacionDestinoResolver.ENTIDAD_REPORTE_PUBLICACION),
                eq(55L),
                isNull());

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacionService, times(2)).crear(
                any(Usuario.class), isNull(),
                eq(NotificacionService.TIPO_PUBLICACION_REPORTADA),
                mensajeCaptor.capture(),
                eq(NotificacionDestinoResolver.ENTIDAD_REPORTE_PUBLICACION),
                eq(55L), isNull());

        assertThat(mensajeCaptor.getValue())
                .contains("Luna Pérez")
                .contains("Luna Diseños")
                .contains("Publicación de prueba")
                .contains("Perfil Dueño")
                .contains("Contenido inapropiado")
                .contains("Contenido ofensivo");

        verify(notificacionService).registrarTodas(anyList());
    }

    @Test
    void alCrearReporte_sinAdmins_noCreaNotificaciones() {
        when(usuarioRepository.findAdministradoresActivos()).thenReturn(List.of());

        new ReportePublicacionNotificacionListener(usuarioRepository, notificacionService)
                .alCrearReporte(evento);

        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void alCrearReporte_descripcionNula_usaTextoPorDefecto() {
        when(usuarioRepository.findAdministradoresActivos()).thenReturn(List.of(admin(90L)));
        when(notificacionService.crear(
                any(Usuario.class), isNull(), anyString(), anyString(), anyString(), any(), isNull()))
                .thenReturn(Notificacion.builder().build());

        ReportePublicacionCreadoEvent sinDescripcion = new ReportePublicacionCreadoEvent(
                56L, 30L, "Publicación de prueba", 20L, "Perfil Dueño",
                1L, "Luna Pérez", 10L, "Luna Diseños",
                MotivoReportePublicacion.SPAM, null);

        new ReportePublicacionNotificacionListener(usuarioRepository, notificacionService)
                .alCrearReporte(sinDescripcion);

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(
                any(Usuario.class), isNull(),
                eq(NotificacionService.TIPO_PUBLICACION_REPORTADA),
                mensajeCaptor.capture(),
                eq(NotificacionDestinoResolver.ENTIDAD_REPORTE_PUBLICACION),
                eq(56L), isNull());

        assertThat(mensajeCaptor.getValue()).contains("Sin descripción");
    }

    private Usuario admin(Long id) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre("Admin")
                .apellido("Sistema")
                .estado(EstadoUsuario.ACTIVO)
                .rolGlobal(RolGlobal.builder()
                        .codigo("ADMINISTRADOR")
                        .nombre("Administrador")
                        .build())
                .build();
    }
}
