package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.RangoFechasPublicacionInvalidoException;
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
class ListarPublicacionesPerfilServiceTest {

    @Mock
    private VerPerfilService verPerfilService;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private PublicacionInteraccionResumenService interaccionResumenService;

    private ListarPublicacionesPerfilService servicio;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        servicio = new ListarPublicacionesPerfilService(
                verPerfilService,
                publicacionRepository,
                proyectoVisibilidadService,
                interaccionResumenService);

        when(interaccionResumenService.resumir(any(), any()))
                .thenReturn(java.util.Map.of());

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(perfil, false));
    }

    @Test
    void listar_devuelveSoloPublicacionesActivasConImagenesOrdenadas() {
        Publicacion publicacion = publicacionConProyecto(null);
        when(publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        eq(7L), eq(EstadoPublicacion.ACTIVA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacion)));

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(7L, 1L, 2L, 0, 10);

        assertEquals(1, response.contenido().size());
        PublicacionResponse card = response.contenido().get(0);
        assertEquals("Editorial", card.titulo());
        assertEquals(2, card.imagenes().size());
        assertEquals(0, card.imagenes().get(0).orden());
        assertEquals(1, card.imagenes().get(1).orden());
        assertNull(card.proyecto());
        assertTrue(card.colaboradores().isEmpty());
        assertEquals(0, card.cantidadMeGusta());
    }

    @Test
    void listar_conProyectoVisible_incluyeProyecto() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.FINALIZADO)
                .build();
        Publicacion publicacion = publicacionConProyecto(proyecto);
        when(proyectoVisibilidadService.puedeVer(proyecto, 2L)).thenReturn(true);
        when(publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        eq(7L), eq(EstadoPublicacion.ACTIVA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacion)));

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(7L, 1L, 2L, 0, 10);

        assertEquals(25L, response.contenido().get(0).proyecto().idProyecto());
        assertTrue(response.contenido().get(0).proyecto().puedeVerProyecto());
    }

    @Test
    void listar_conProyectoNoVisible_omiteLosDatosDelProyecto() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña privada")
                .estado(EstadoProyecto.FINALIZADO)
                .build();
        Publicacion publicacion = publicacionConProyecto(proyecto);
        when(proyectoVisibilidadService.puedeVer(proyecto, 2L)).thenReturn(false);
        when(publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        eq(7L), eq(EstadoPublicacion.ACTIVA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacion)));

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(7L, 1L, 2L, 0, 10);

        assertNull(response.contenido().get(0).proyecto());
    }

    @Test
    void listar_conFiltros_usaSpecificationYDevuelveResultados() {
        Publicacion publicacion = publicacionConProyecto(null);
        when(publicacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacion)));

        BuscarPublicacionesFiltro filtro = new BuscarPublicacionesFiltro(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null,
                "Editorial",
                null);

        PaginaResponse<PublicacionResponse> response =
                servicio.listar(7L, 1L, 2L, filtro, 0, 10);

        assertEquals(1, response.contenido().size());
        verify(publicacionRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(publicacionRepository, never())
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        any(Long.class), any(EstadoPublicacion.class), any(Pageable.class));
    }

    @Test
    void listar_filtroVacio_usaConsultaSimple() {
        Publicacion publicacion = publicacionConProyecto(null);
        when(publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        eq(7L), eq(EstadoPublicacion.ACTIVA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicacion)));

        servicio.listar(7L, 1L, 2L, new BuscarPublicacionesFiltro(
                null, null, null, "   ", ""), 0, 10);

        verify(publicacionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listar_rangoDeFechasInvalido_lanzaExcepcion() {
        BuscarPublicacionesFiltro filtro = new BuscarPublicacionesFiltro(
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1),
                null,
                null,
                null);

        assertThrows(RangoFechasPublicacionInvalidoException.class,
                () -> servicio.listar(7L, 1L, 2L, filtro, 0, 10));
    }

    private Publicacion publicacionConProyecto(Proyecto proyecto) {
        Imagen primera = Imagen.builder().idImagen(10L).url("/uploads/publicaciones/a.webp").build();
        Imagen segunda = Imagen.builder().idImagen(11L).url("/uploads/publicaciones/b.webp").build();
        return Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial")
                .descripcion("Descripción")
                .fechaPublicacion(LocalDateTime.of(2026, 11, 10, 12, 0))
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .proyecto(proyecto)
                .imagenes(List.of(primera, segunda))
                .build();
    }
}
