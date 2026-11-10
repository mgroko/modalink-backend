package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObtenerPublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private UbicacionService ubicacionService;

    private ObtenerPublicacionService servicio;
    private Perfil perfil;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        PublicacionDatosService datosService = new PublicacionDatosService(
                perfilRepository,
                proyectoRepository,
                proyectoVisibilidadService,
                ubicacionService);
        servicio = new ObtenerPublicacionService(
                publicacionRepository, datosService, proyectoVisibilidadService);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .descripcion("Descripción")
                .perfil(perfil)
                .imagenes(new ArrayList<>(List.of(Imagen.builder()
                        .idImagen(10L)
                        .url("/uploads/publicaciones/a.webp")
                        .nombreArchivo("a.webp")
                        .build())))
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(7L, 1L))
                .thenReturn(Optional.of(perfil));
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.of(publicacion));
        when(proyectoVisibilidadService.puedeVer(any(), anyLong())).thenReturn(true);
    }

    @Test
    void obtener_devuelveElDetalleDeLaPublicacionPropia() {
        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial Otoño", response.titulo());
        assertEquals(1, response.imagenes().size());
    }

    @Test
    void obtener_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.obtener(1L, null, 99L));
    }

    @Test
    void obtener_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.obtener(1L, 7L, 99L));
    }

    @Test
    void obtener_publicacionInexistenteOAjena_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.obtener(1L, 7L, 99L));
    }

    @Test
    void obtener_proyectoNoVisible_loIncluyeParaPoderConservarlo() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña privada")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        publicacion.setProyecto(proyecto);
        when(proyectoVisibilidadService.puedeVer(proyecto, 7L)).thenReturn(false);

        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertNotNull(response.proyecto());
        assertEquals(25L, response.proyecto().idProyecto());
        assertFalse(response.proyecto().puedeVerProyecto());
    }

    @Test
    void obtener_mapeaSoloColaboradoresActivos() {
        Perfil activo = Perfil.builder()
                .idPerfil(8L)
                .nombreArtistico("Sol")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .build();
        Perfil inactivo = Perfil.builder()
                .idPerfil(9L)
                .nombreArtistico("Baja")
                .estado(EstadoPerfil.Baja)
                .profesion(Profesion.builder().idProfesion(4L).nombre("Fotógrafa").build())
                .build();
        publicacion.setColaboradores(new LinkedHashSet<>(List.of(activo, inactivo)));

        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertEquals(1, response.colaboradores().size());
        assertEquals(8L, response.colaboradores().get(0).idPerfil());
    }

    @Test
    void obtener_proyectoVisible_loDevuelveConLaBandera() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        publicacion.setProyecto(proyecto);
        when(proyectoVisibilidadService.puedeVer(proyecto, 7L)).thenReturn(true);

        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertNotNull(response.proyecto());
        assertEquals("Campaña", response.proyecto().nombre());
        assertEquals(true, response.proyecto().puedeVerProyecto());
    }

    @Test
    void obtener_conProyectoNulo_noConsultaVisibilidad() {
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(eq(99L), eq(7L)))
                .thenReturn(Optional.of(publicacion));

        PublicacionResponse response = servicio.obtener(1L, 7L, 99L);

        assertEquals(null, response.proyecto());
    }
}
