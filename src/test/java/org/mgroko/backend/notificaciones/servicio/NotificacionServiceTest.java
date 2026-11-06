package org.mgroko.backend.notificaciones.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResumenResponse;
import org.mgroko.backend.notificaciones.exception.NotificacionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private NotificacionDestinoResolver notificacionDestinoResolver;

    @InjectMocks
    private NotificacionService notificacionService;

    private Usuario usuario;
    private Perfil perfil;
    private Notificacion notificacion;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(10L).build();
        perfil = Perfil.builder().idPerfil(20L).usuario(usuario).build();
        notificacion = Notificacion.builder()
                .idNotificacion(30L)
                .usuarioDestino(usuario)
                .perfilContexto(perfil)
                .tipo("HITO_CUMPLIDO")
                .mensaje("Hito completado")
                .entidadTipo("HITO")
                .entidadId(40L)
                .leida(false)
                .fechaGenerada(LocalDateTime.of(2026, 10, 21, 12, 0))
                .build();
    }

    @Test
    void listar_sinPerfil_consultaTodasLasDelUsuario() {
        Page<Notificacion> pagina = new PageImpl<>(List.of(notificacion));
        when(notificacionRepository.findByUsuarioDestino_IdUsuario(eq(10L), any(Pageable.class)))
                .thenReturn(pagina);

        PaginaResponse<NotificacionResponse> respuesta =
                notificacionService.listar(10L, null, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().get(0).idPerfilContexto()).isEqualTo(20L);
        verify(notificacionRepository, never())
                .findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(any(), any(), any());
    }

    @Test
    void listar_conPerfilPropio_consultaSoloEsePerfil() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(20L, 10L))
                .thenReturn(Optional.of(perfil));
        when(notificacionRepository.findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                eq(10L), eq(20L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificacion)));

        PaginaResponse<NotificacionResponse> respuesta =
                notificacionService.listar(10L, 20L, 0, 20);

        assertThat(respuesta.contenido()).hasSize(1);
        verify(notificacionRepository, never())
                .findByUsuarioDestino_IdUsuario(any(), any());
    }

    @Test
    void listar_incluyeIdProyectoResuelto() {
        when(notificacionRepository.findByUsuarioDestino_IdUsuario(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificacion)));
        when(notificacionDestinoResolver.resolverProyectos(any()))
                .thenReturn(Map.of(30L, 50L));

        PaginaResponse<NotificacionResponse> respuesta =
                notificacionService.listar(10L, null, 0, 20);

        assertThat(respuesta.contenido().get(0).idProyecto()).isEqualTo(50L);
    }

    @Test
    void listar_sinProyectoResuelto_devuelveNull() {
        when(notificacionRepository.findByUsuarioDestino_IdUsuario(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificacion)));

        PaginaResponse<NotificacionResponse> respuesta =
                notificacionService.listar(10L, null, 0, 20);

        assertThat(respuesta.contenido().get(0).idProyecto()).isNull();
    }

    @Test
    void listar_conPerfilAjeno_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(99L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.listar(10L, 99L, 0, 20))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verify(notificacionRepository, never()).findByUsuarioDestino_IdUsuario(any(), any());
    }

    @Test
    void resumen_sinPerfil_devuelveTotalesDelUsuario() {
        when(notificacionRepository.countByUsuarioDestino_IdUsuario(10L)).thenReturn(5L);
        when(notificacionRepository.countByUsuarioDestino_IdUsuarioAndLeidaFalse(10L)).thenReturn(2L);

        NotificacionResumenResponse resumen = notificacionService.resumen(10L, null);

        assertThat(resumen.total()).isEqualTo(5);
        assertThat(resumen.noLeidas()).isEqualTo(2);
    }

    @Test
    void resumen_conPerfil_devuelveTotalesDelPerfil() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(20L, 10L))
                .thenReturn(Optional.of(perfil));
        when(notificacionRepository.countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(10L, 20L))
                .thenReturn(3L);
        when(notificacionRepository
                .countByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfilAndLeidaFalse(10L, 20L))
                .thenReturn(1L);

        NotificacionResumenResponse resumen = notificacionService.resumen(10L, 20L);

        assertThat(resumen.total()).isEqualTo(3);
        assertThat(resumen.noLeidas()).isEqualTo(1);
    }

    @Test
    void marcarLeida_propia_seteaFechaYGuarda() {
        when(notificacionRepository.findByIdNotificacionAndUsuarioDestino_IdUsuario(30L, 10L))
                .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(notificacion)).thenAnswer(invocation -> invocation.getArgument(0));

        NotificacionResponse respuesta = notificacionService.marcarLeida(10L, 30L);

        assertThat(respuesta.leida()).isTrue();
        assertThat(respuesta.fechaLeida()).isNotNull();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarLeida_incluyeIdProyectoResuelto() {
        when(notificacionRepository.findByIdNotificacionAndUsuarioDestino_IdUsuario(30L, 10L))
                .thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(notificacion))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionDestinoResolver.resolverProyecto(notificacion)).thenReturn(50L);

        NotificacionResponse respuesta = notificacionService.marcarLeida(10L, 30L);

        assertThat(respuesta.idProyecto()).isEqualTo(50L);
    }

    @Test
    void marcarLeida_yaLeida_noVuelveAGuardar() {
        notificacion.setLeida(true);
        notificacion.setFechaLeida(LocalDateTime.of(2026, 10, 20, 9, 0));
        when(notificacionRepository.findByIdNotificacionAndUsuarioDestino_IdUsuario(30L, 10L))
                .thenReturn(Optional.of(notificacion));

        NotificacionResponse respuesta = notificacionService.marcarLeida(10L, 30L);

        assertThat(respuesta.fechaLeida()).isEqualTo(LocalDateTime.of(2026, 10, 20, 9, 0));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void marcarLeida_ajena_lanzaExcepcion() {
        when(notificacionRepository.findByIdNotificacionAndUsuarioDestino_IdUsuario(30L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(99L, 30L))
                .isInstanceOf(NotificacionNoEncontradaException.class);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void marcarTodas_sinPerfil_actualizaTodasLasDelUsuario() {
        when(notificacionRepository.marcarTodasLeidas(eq(10L), any(LocalDateTime.class)))
                .thenReturn(4);

        int actualizadas = notificacionService.marcarTodasLeidas(10L, null);

        assertThat(actualizadas).isEqualTo(4);
        verify(notificacionRepository, never())
                .marcarTodasLeidasDePerfil(any(), any(), any());
    }

    @Test
    void marcarTodas_conPerfil_actualizaSoloEsePerfil() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(20L, 10L))
                .thenReturn(Optional.of(perfil));
        when(notificacionRepository.marcarTodasLeidasDePerfil(
                eq(10L), eq(20L), any(LocalDateTime.class)))
                .thenReturn(2);

        int actualizadas = notificacionService.marcarTodasLeidas(10L, 20L);

        assertThat(actualizadas).isEqualTo(2);
        verify(notificacionRepository, never()).marcarTodasLeidas(any(), any());
    }

    @Test
    void crear_conPerfil_guardaContextoYOrigenServicio() {
        Notificacion creada = notificacionService.crear(
                usuario, perfil, "PROYECTO_CANCELADO", "Mensaje", "PROYECTO", 50L);

        assertThat(creada.getPerfilContexto()).isEqualTo(perfil);
        assertThat(creada.getOrigen()).isEqualTo(NotificacionService.ORIGEN_SERVICIO);
        assertThat(creada.getLeida()).isFalse();
        assertThat(creada.getFechaLeida()).isNull();
    }

    @Test
    void crear_conProyecto_guardaIdProyecto() {
        Notificacion creada = notificacionService.crear(
                usuario, perfil, "ACTIVIDAD_ELIMINADA", "Mensaje", "ACTIVIDAD", 60L, 50L);

        assertThat(creada.getIdProyecto()).isEqualTo(50L);
        assertThat(creada.getEntidadTipo()).isEqualTo("ACTIVIDAD");
        assertThat(creada.getEntidadId()).isEqualTo(60L);
    }
}
