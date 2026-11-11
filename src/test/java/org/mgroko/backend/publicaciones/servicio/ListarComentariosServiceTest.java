package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Comentario;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarComentariosServiceTest {

    @Mock
    private PublicacionInteraccionService interaccionService;

    @Mock
    private ComentarioRepository comentarioRepository;

    private ListarComentariosService servicio;
    private Comentario comentario;

    @BeforeEach
    void setUp() {
        servicio = new ListarComentariosService(
                interaccionService, comentarioRepository);

        Perfil perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .imagen(Imagen.builder()
                        .idImagen(1L)
                        .url("/uploads/perfiles/luna.webp")
                        .build())
                .build();

        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .build();

        comentario = Comentario.builder()
                .idComentario(10L)
                .texto("Muy buena publicación")
                .fechaComentario(LocalDateTime.of(2026, 11, 11, 12, 0))
                .publicacion(publicacion)
                .perfil(perfil)
                .build();
    }

    @Test
    void listar_devuelvePaginaConComentariosYResuelveVisibilidad() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comentario> pagina = new PageImpl<>(List.of(comentario), pageable, 1);

        when(comentarioRepository
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), any(Pageable.class)))
                .thenReturn(pagina);

        PaginaResponse<ComentarioResponse> response =
                servicio.listar(1L, 7L, 99L, 0, 10);

        assertEquals(1, response.contenido().size());
        assertEquals("Muy buena publicación", response.contenido().get(0).texto());
        assertEquals(7L, response.contenido().get(0).autor().idPerfil());
        verify(interaccionService).resolver(1L, 7L, 99L);
    }

    @Test
    void listar_paginaVacia_devuelvePaginaConCeroElementos() {
        when(comentarioRepository
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), any(Pageable.class)))
                .thenReturn(Page.empty());

        PaginaResponse<ComentarioResponse> response =
                servicio.listar(1L, 7L, 99L, 0, 10);

        assertTrue(response.contenido().isEmpty());
        assertEquals(0, response.totalElementos());
    }

    @Test
    void listar_tamanioMayorAlMaximo_seLimitaA50() {
        when(comentarioRepository
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), any(Pageable.class)))
                .thenReturn(Page.empty());

        servicio.listar(1L, 7L, 99L, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(comentarioRepository)
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void listar_tamanioInvalido_usaDiezPorDefecto() {
        when(comentarioRepository
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), any(Pageable.class)))
                .thenReturn(Page.empty());

        servicio.listar(1L, 7L, 99L, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(comentarioRepository)
                .findByPublicacion_IdPublicacionOrderByFechaComentarioAscIdComentarioAsc(
                        eq(99L), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
    }
}
