package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObtenerPublicacionDetalleServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PublicacionDatosService publicacionDatosService;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private VerPerfilService verPerfilService;

    private ObtenerPublicacionDetalleService servicio;
    private Perfil autor;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        servicio = new ObtenerPublicacionDetalleService(
                publicacionRepository,
                publicacionDatosService,
                proyectoVisibilidadService,
                verPerfilService);

        autor = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .descripcion("Producción realizada en Buenos Aires.")
                .fechaPublicacion(LocalDateTime.of(2026, 11, 10, 14, 30))
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(autor)
                .imagenes(List.of(Imagen.builder()
                        .idImagen(10L)
                        .url("/uploads/publicaciones/portada.webp")
                        .build()))
                .build();
    }

    @Test
    void obtener_publicacionPropia_devuelveRespuestaPropia() {
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.of(publicacion));
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(autor, true));

        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial Otoño", response.titulo());
        assertEquals(7L, response.autor().idPerfil());
        assertEquals(1, response.imagenes().size());
    }

    @Test
    void obtener_publicacionAjenaVisible_devuelveDetalle() {
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.of(publicacion));
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(autor, false));

        PublicacionResponse response = servicio.obtener(1L, 8L, 99L);

        assertEquals(99L, response.idPublicacion());
        assertEquals("Luna Diseños", response.autor().nombreArtistico());
    }

    @Test
    void obtener_proyectoVisible_loIncluye() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña Otoño")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        publicacion.setProyecto(proyecto);
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.of(publicacion));
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenReturn(new VerPerfilService.PerfilVisible(autor, false));
        when(proyectoVisibilidadService.puedeVer(proyecto, 8L)).thenReturn(true);

        PublicacionResponse response = servicio.obtener(1L, 8L, 99L);

        assertNotNull(response.proyecto());
        assertEquals("Campaña Otoño", response.proyecto().nombre());
    }

    @Test
    void obtener_publicacionNoActiva_lanzaNoEncontrada() {
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.of(publicacion));

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.obtener(1L, 8L, 99L));
    }

    @Test
    void obtener_publicacionInexistente_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.obtener(1L, 8L, 99L));
    }

    @Test
    void obtener_autorNoVisible_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacion(99L))
                .thenReturn(Optional.of(publicacion));
        when(verPerfilService.obtenerPerfilVisible(7L, 1L))
                .thenThrow(new org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException());

        assertThrows(org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException.class,
                () -> servicio.obtener(1L, 8L, 99L));
    }

    @Test
    void obtener_sinPerfilActivo_lanzaExcepcion() {
        doThrow(new PerfilActivoNoSeleccionadoException())
                .when(publicacionDatosService)
                .validarPerfilActivo(1L, null);

        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.obtener(1L, null, 99L));
    }

    @Test
    void obtener_perfilActivoEnBaja_lanzaExcepcion() {
        doThrow(new PerfilEnBajaException("El perfil no se encuentra activo."))
                .when(publicacionDatosService)
                .validarPerfilActivo(1L, 8L);

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.obtener(1L, 8L, 99L));
    }
}
