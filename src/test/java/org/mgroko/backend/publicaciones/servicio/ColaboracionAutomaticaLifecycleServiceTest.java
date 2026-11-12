package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ColaboracionAutomaticaLifecycleServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ColaboracionAutomaticaRepository colaboracionRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ColaboradorPublicacionOrigenService colaboradorOrigenService;

    @Mock
    private NotificacionService notificacionService;

    private ColaboracionAutomaticaLifecycleService servicio;
    private ColaboracionAutomatica colaboracion;

    @BeforeEach
    void setUp() {
        servicio = new ColaboracionAutomaticaLifecycleService(
                colaboracionRepository,
                publicacionRepository,
                colaboradorOrigenService,
                notificacionService,
                RELOJ);

        Perfil perfilA = perfil(7L, 1L, "Luna");
        Perfil perfilB = perfil(8L, 2L, "Sol");
        Publicacion publicacionA = publicacion(10L, perfilA, "A");
        Publicacion publicacionB = publicacion(20L, perfilB, "B");

        colaboracion = ColaboracionAutomatica.builder()
                .idColaboracionAutomatica(50L)
                .publicacionA(publicacionA)
                .publicacionB(publicacionB)
                .perfilA(perfilA)
                .perfilB(perfilB)
                .estado(EstadoColaboracionAutomatica.PENDIENTE)
                .build();

        when(colaboracionRepository.findByPublicacionYEstado(
                10L, EstadoColaboracionAutomatica.PENDIENTE))
                .thenReturn(List.of(colaboracion));
        when(colaboracionRepository.findByImagenYEstado(
                99L, EstadoColaboracionAutomatica.PENDIENTE))
                .thenReturn(List.of(colaboracion));
        when(colaboracionRepository.findConfirmadaPorPublicacionYPerfil(
                20L, 7L, EstadoColaboracionAutomatica.CONFIRMADA))
                .thenReturn(List.of(colaboracion));
        when(colaboracionRepository.saveAndFlush(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class), any(), any(), any(), anyLong()))
                .thenReturn(mock(Notificacion.class));
    }

    @Test
    void cancelarPorPublicacion_marcaCanceladaYlimpiaNotificaciones() {
        servicio.cancelarPendientesPorPublicacion(10L);

        assertEquals(EstadoColaboracionAutomatica.CANCELADA, colaboracion.getEstado());
        verify(notificacionService).eliminarPorEntidad(
                NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA, 50L);
    }

    @Test
    void cancelarPorImagen_marcaCancelada() {
        servicio.cancelarPendientesPorImagen(99L);

        assertEquals(EstadoColaboracionAutomatica.CANCELADA, colaboracion.getEstado());
    }

    @Test
    void disolverPorEtiqueta_quitaEtiquetaReciprocaYNotifica() {
        colaboracion.setEstado(EstadoColaboracionAutomatica.CONFIRMADA);

        servicio.disolverPorEtiqueta(20L, 7L);

        assertEquals(EstadoColaboracionAutomatica.DISUELTA, colaboracion.getEstado());
        verify(publicacionRepository).eliminarColaboradorPublicacion(10L, 8L);
        verify(colaboradorOrigenService).eliminar(10L, 8L);
        verify(notificacionService).registrarTodas(any());
    }

    private Perfil perfil(Long id, Long idUsuario, String nombre) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .build();
    }

    private Publicacion publicacion(Long id, Perfil perfil, String titulo) {
        return Publicacion.builder()
                .idPublicacion(id)
                .titulo(titulo)
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build();
    }
}
