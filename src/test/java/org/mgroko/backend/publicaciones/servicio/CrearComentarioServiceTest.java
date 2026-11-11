package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.dto.CrearComentarioRequest;
import org.mgroko.backend.publicaciones.exception.ComentarioInvalidoException;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearComentarioServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicacionInteraccionService interaccionService;

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private PublicacionInteraccionNotificacionService notificacionService;

    private CrearComentarioService servicio;
    private Perfil perfil;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new CrearComentarioService(
                interaccionService,
                comentarioRepository,
                notificacionService,
                RELOJ);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .imagen(Imagen.builder()
                        .idImagen(1L)
                        .url("/uploads/perfiles/luna.webp")
                        .build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .perfil(perfil)
                .build();

        when(interaccionService.resolver(1L, 7L, 99L))
                .thenReturn(new PublicacionInteraccionService.ContextoInteraccionPublicacion(
                        perfil, publicacion));

        when(comentarioRepository.saveAndFlush(any(Comentario.class)))
                .thenAnswer(invocacion -> {
                    Comentario comentario = invocacion.getArgument(0);
                    comentario.setIdComentario(10L);
                    return comentario;
                });
    }

    @Test
    void crear_registraTextoNormalizadoYNotifica() {
        ComentarioResponse response = servicio.crear(
                1L,
                7L,
                99L,
                new CrearComentarioRequest("  Muy buena publicación  "));

        assertEquals(10L, response.idComentario());
        assertEquals(99L, response.idPublicacion());
        assertEquals("Muy buena publicación", response.texto());
        assertEquals(7L, response.autor().idPerfil());
        assertEquals(LocalDateTime.of(2026, 11, 11, 12, 0), response.fechaComentario());

        ArgumentCaptor<Comentario> captor = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarioRepository).saveAndFlush(captor.capture());
        assertEquals("Muy buena publicación", captor.getValue().getTexto());
        assertEquals(publicacion, captor.getValue().getPublicacion());
        assertEquals(perfil, captor.getValue().getPerfil());

        verify(notificacionService).notificarComentario(publicacion, perfil);
    }

    @Test
    void crear_textoVacio_lanzaErrorYNoPersisteNiNotifica() {
        assertThrows(ComentarioInvalidoException.class,
                () -> servicio.crear(1L, 7L, 99L, new CrearComentarioRequest("   ")));

        verify(comentarioRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).notificarComentario(any(), any());
    }

    @Test
    void crear_textoNulo_lanzaError() {
        assertThrows(ComentarioInvalidoException.class,
                () -> servicio.crear(1L, 7L, 99L, new CrearComentarioRequest(null)));

        verify(comentarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_textoMayorAlLimite_lanzaError() {
        String texto = "a".repeat(CrearComentarioService.LIMITE_TEXTO + 1);

        assertThrows(ComentarioInvalidoException.class,
                () -> servicio.crear(1L, 7L, 99L, new CrearComentarioRequest(texto)));

        verify(comentarioRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).notificarComentario(any(), any());
    }

    @Test
    void crear_textoEnElLimite_esValido() {
        String texto = "a".repeat(CrearComentarioService.LIMITE_TEXTO);

        ComentarioResponse response = servicio.crear(
                1L, 7L, 99L, new CrearComentarioRequest(texto));

        assertEquals(CrearComentarioService.LIMITE_TEXTO, response.texto().length());
        verify(notificacionService).notificarComentario(
                eq(publicacion), eq(perfil));
    }
}
