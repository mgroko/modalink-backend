package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarFeedPublicacionesServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private PublicacionInteraccionResumenService interaccionResumenService;

    private ListarFeedPublicacionesService servicio;
    private Perfil perfilActivo;

    @BeforeEach
    void setUp() {
        servicio = new ListarFeedPublicacionesService(
                perfilRepository,
                conexionRepository,
                publicacionRepository,
                proyectoVisibilidadService,
                interaccionResumenService);

        when(interaccionResumenService.resumir(any(), any()))
                .thenReturn(java.util.Map.of());

        perfilActivo = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(7L, 1L))
                .thenReturn(Optional.of(perfilActivo));
    }

    @Test
    void listar_incluyePublicacionesDeContactosYPropias() {
        when(conexionRepository.buscarIdsContrapartesActivas(7L)).thenReturn(List.of(8L));
        when(publicacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacionDe(perfilActivo))));

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(1L, 7L, true, 0, 10);

        assertEquals(1, response.contenido().size());
        verify(publicacionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listar_sinContactosYSinPropias_devuelvePaginaVaciaSinConsultarPublicaciones() {
        when(conexionRepository.buscarIdsContrapartesActivas(7L)).thenReturn(List.of());

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(1L, 7L, false, 0, 10);

        assertTrue(response.contenido().isEmpty());
        assertEquals(0, response.totalElementos());
        verify(publicacionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listar_sinContactosPeroConPropias_consultaPublicacionesPropias() {
        when(conexionRepository.buscarIdsContrapartesActivas(7L)).thenReturn(List.of());
        when(publicacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacionDe(perfilActivo))));

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(1L, 7L, true, 0, 10);

        assertEquals(1, response.contenido().size());
        verify(publicacionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listar_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.listar(1L, null, true, 0, 10));
    }

    @Test
    void listar_perfilEnBaja_lanzaExcepcion() {
        perfilActivo.setEstado(EstadoPerfil.PendienteBaja);

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.listar(1L, 7L, true, 0, 10));
    }

    private Publicacion publicacionDe(Perfil perfil) {
        Imagen imagen = Imagen.builder()
                .idImagen(10L)
                .url("/uploads/publicaciones/a.webp")
                .build();
        return Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial")
                .descripcion("Descripción")
                .fechaPublicacion(LocalDateTime.of(2026, 11, 10, 12, 0))
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .imagenes(List.of(imagen))
                .build();
    }
}
