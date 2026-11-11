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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.publicaciones.dto.MeGustaResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionarMeGustaPublicacionServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicacionInteraccionService interaccionService;

    @Mock
    private MeGustaRepository meGustaRepository;

    @Mock
    private PublicacionInteraccionNotificacionService notificacionService;

    private GestionarMeGustaPublicacionService servicio;
    private Perfil perfil;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new GestionarMeGustaPublicacionService(
                interaccionService,
                meGustaRepository,
                notificacionService,
                RELOJ);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(1L)
                        .estado(EstadoUsuario.Activo)
                        .build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build();

        when(interaccionService.resolver(1L, 7L, 99L))
                .thenReturn(new PublicacionInteraccionService.ContextoInteraccionPublicacion(
                        perfil, publicacion));
    }

    @Test
    void dar_cuandoNoExistia_creaYNotifica() {
        when(meGustaRepository.insertarSiNoExiste(eq(99L), eq(7L), any(LocalDateTime.class)))
                .thenReturn(1);
        when(meGustaRepository.contarPorPublicacion(99L)).thenReturn(5L);

        MeGustaResponse response = servicio.dar(1L, 7L, 99L);

        assertTrue(response.meGusta());
        assertEquals(5, response.cantidadMeGusta());
        assertEquals(99L, response.idPublicacion());

        ArgumentCaptor<LocalDateTime> fecha = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(meGustaRepository).insertarSiNoExiste(eq(99L), eq(7L), fecha.capture());
        assertEquals(LocalDateTime.of(2026, 11, 11, 12, 0), fecha.getValue());

        verify(notificacionService).notificarMeGusta(publicacion, perfil);
    }

    @Test
    void dar_cuandoYaExistia_esIdempotenteYNoNotifica() {
        when(meGustaRepository.insertarSiNoExiste(eq(99L), eq(7L), any(LocalDateTime.class)))
                .thenReturn(0);
        when(meGustaRepository.contarPorPublicacion(99L)).thenReturn(5L);

        MeGustaResponse response = servicio.dar(1L, 7L, 99L);

        assertTrue(response.meGusta());
        assertEquals(5, response.cantidadMeGusta());
        verify(notificacionService, never()).notificarMeGusta(any(), any());
    }

    @Test
    void quitar_eliminaSinNotificar() {
        when(meGustaRepository.contarPorPublicacion(99L)).thenReturn(4L);

        MeGustaResponse response = servicio.quitar(1L, 7L, 99L);

        assertFalse(response.meGusta());
        assertEquals(4, response.cantidadMeGusta());
        verify(meGustaRepository).eliminar(99L, 7L);
        verify(notificacionService, never()).notificarMeGusta(any(), any());
    }

    @Test
    void dar_publicacionNoVisible_propagaLaExcepcion() {
        when(interaccionService.resolver(1L, 7L, 99L))
                .thenThrow(new PublicacionNoEncontradaException(99L));

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.dar(1L, 7L, 99L));

        verify(meGustaRepository, never())
                .insertarSiNoExiste(any(), any(), any());
    }
}
