package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.ProyectoNoAsociableException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModificarPublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private StorageService storageService;

    @Mock
    private BorradoArchivosDiferido borradoArchivosDiferido;

    @Mock
    private EntityManager entityManager;

    private ModificarPublicacionService servicio;
    private Perfil perfil;
    private Publicacion publicacion;
    private Imagen imagen1;
    private Imagen imagen2;

    @BeforeEach
    void setUp() {
        PublicacionDatosService datosService = new PublicacionDatosService(
                perfilRepository,
                proyectoRepository,
                proyectoVisibilidadService,
                ubicacionService);

        servicio = new ModificarPublicacionService(
                publicacionRepository,
                imagenRepository,
                datosService,
                proyectoVisibilidadService,
                storageService,
                borradoArchivosDiferido,
                entityManager);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();

        imagen1 = Imagen.builder()
                .idImagen(10L)
                .nombreArchivo("publicacion_a.webp")
                .url("/uploads/publicaciones/publicacion_a.webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();
        imagen2 = Imagen.builder()
                .idImagen(20L)
                .nombreArchivo("publicacion_b.webp")
                .url("/uploads/publicaciones/publicacion_b.webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();

        publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .titulo("Editorial Otoño")
                .descripcion("Descripción original")
                .perfil(perfil)
                .imagenes(new ArrayList<>(List.of(imagen1, imagen2)))
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(7L, 1L))
                .thenReturn(Optional.of(perfil));
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(Publicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(publicacionRepository.saveAndFlush(any(Publicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(30L);
            return imagen;
        });
        when(storageService.almacenarImagenPublicacion(any()))
                .thenReturn(new ArchivoAlmacenado(
                        "publicacion_nueva.webp",
                        "/uploads/publicaciones/publicacion_nueva.webp",
                        "image/webp",
                        100L));
        when(proyectoVisibilidadService.puedeVer(any(), anyLong())).thenReturn(true);
    }

    private ModificarPublicacionRequest request(String titulo, List<Long> conservadas) {
        ModificarPublicacionRequest request = new ModificarPublicacionRequest();
        request.setTitulo(titulo);
        request.setDescripcion("Descripción nueva");
        request.setImagenesConservadas(conservadas);
        request.setArchivos(List.of());
        return request;
    }

    private MockMultipartFile archivo(String nombre) {
        return new MockMultipartFile("archivos", nombre, "image/jpeg", "bytes".getBytes());
    }

    @Test
    void modificar_actualizaDatosYConservaImagenesEnElOrdenIndicado() {
        ModificarPublicacionRequest request = request("  Editorial Primavera  ", List.of(20L, 10L));

        PublicacionResponse response = servicio.modificar(1L, 7L, 99L, request);

        assertEquals("Editorial Primavera", response.titulo());
        assertEquals("Descripción nueva", response.descripcion());
        assertEquals(List.of(20L, 10L),
                response.imagenes().stream().map(i -> i.idImagen()).toList());
        assertEquals("Editorial Primavera", publicacion.getTitulo());
        verify(imagenRepository, never()).eliminarFisicamente(anyLong());
    }

    @Test
    void modificar_tituloVacio_lanzaExcepcionYNoGuarda() {
        ModificarPublicacionRequest request = request("   ", List.of(10L));

        assertThrows(PublicacionInvalidaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));

        verify(publicacionRepository, never()).save(any(Publicacion.class));
    }

    @Test
    void modificar_descripcionVacia_seGuardaComoNull() {
        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setDescripcion("   ");

        PublicacionResponse response = servicio.modificar(1L, 7L, 99L, request);

        assertNull(response.descripcion());
        assertNull(publicacion.getDescripcion());
    }

    @Test
    void modificar_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.modificar(1L, null, 99L, request("Editorial", List.of(10L))));
    }

    @Test
    void modificar_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request("Editorial", List.of(10L))));
    }

    @Test
    void modificar_publicacionInexistenteOAjena_lanzaNoEncontrada() {
        when(publicacionRepository.findByIdPublicacionAndPerfilIdPerfil(99L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request("Editorial", List.of(10L))));
    }

    @Test
    void modificar_sinImagenesFinales_lanzaExcepcion() {
        ModificarPublicacionRequest request = request("Editorial", List.of());

        assertThrows(PublicacionInvalidaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));
    }

    @Test
    void modificar_eliminaLasImagenesNoConservadas() {
        PublicacionResponse response = servicio.modificar(
                1L, 7L, 99L, request("Editorial", List.of(10L)));

        assertEquals(1, response.imagenes().size());
        verify(borradoArchivosDiferido).registrarBorrados(null, "publicacion_b.webp");
        verify(imagenRepository).eliminarFisicamente(imagen2.getIdImagen());
        verify(imagenRepository, never()).eliminarFisicamente(imagen1.getIdImagen());
    }

    @Test
    void modificar_agregaImagenesNuevasAlFinal() {
        ModificarPublicacionRequest request = request("Editorial", List.of(10L, 20L));
        request.setArchivos(List.of(archivo("nueva.jpg")));

        PublicacionResponse response = servicio.modificar(1L, 7L, 99L, request);

        assertEquals(3, response.imagenes().size());
        assertEquals(30L, response.imagenes().get(2).idImagen());
        verify(borradoArchivosDiferido).registrarBorrados("publicacion_nueva.webp", null);
    }

    @Test
    void modificar_masDeDiezImagenesFinales_lanzaExcepcion() {
        ModificarPublicacionRequest request = request("Editorial", List.of(10L, 20L));
        request.setArchivos(java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> (MultipartFile) archivo("foto" + i + ".jpg"))
                .toList());

        assertThrows(LimiteImagenesPublicacionExcedidoException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));
    }

    @Test
    void modificar_imagenDeOtraPublicacion_lanzaExcepcion() {
        assertThrows(PublicacionInvalidaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request("Editorial", List.of(999L))));
    }

    @Test
    void modificar_desvinculaProyecto() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        publicacion.setProyecto(proyecto);

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setIdProyecto(null);

        PublicacionResponse response = servicio.modificar(1L, 7L, 99L, request);

        assertNull(response.proyecto());
        assertNull(publicacion.getProyecto());
    }

    @Test
    void modificar_mismoProyecto_noRevalidaMembresia() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        publicacion.setProyecto(proyecto);

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setIdProyecto(25L);

        servicio.modificar(1L, 7L, 99L, request);

        verify(proyectoRepository, never()).findById(anyLong());
        verify(proyectoVisibilidadService, never()).esMiembroActivo(anyLong(), anyLong());
    }

    @Test
    void modificar_proyectoNuevoYNoMiembro_lanzaNoAsociable() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        when(proyectoRepository.findById(25L)).thenReturn(Optional.of(proyecto));
        when(proyectoVisibilidadService.esMiembroActivo(25L, 7L)).thenReturn(false);

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setIdProyecto(25L);

        assertThrows(ProyectoNoAsociableException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));
    }

    @Test
    void modificar_proyectoInexistente_lanzaNoEncontrado() {
        when(proyectoRepository.findById(25L)).thenReturn(Optional.empty());

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setIdProyecto(25L);

        assertThrows(ProyectoNoEncontradoException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));
    }

    @Test
    void modificar_actualizaUbicacion() {
        Ubicacion ubicacion = Ubicacion.builder().idUbicacion(5L).build();
        when(ubicacionService.obtenerOCrear("0602800000", "06")).thenReturn(ubicacion);

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setLocalidadId("0602800000");
        request.setProvinciaId("06");

        servicio.modificar(1L, 7L, 99L, request);

        assertEquals(ubicacion, publicacion.getUbicacion());
    }

    @Test
    void modificar_sinLocalidad_limpiaUbicacion() {
        publicacion.setUbicacion(Ubicacion.builder().idUbicacion(5L).build());

        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setLocalidadId(" ");
        request.setProvinciaId(null);

        servicio.modificar(1L, 7L, 99L, request);

        assertNull(publicacion.getUbicacion());
        verify(ubicacionService, never()).obtenerOCrear(any(), any());
    }

    @Test
    void modificar_sinImagenesConservadasEnElRequest_lanzaExcepcion() {
        ModificarPublicacionRequest request = request("Editorial", null);

        assertThrows(PublicacionInvalidaException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));
    }

    @Test
    void modificar_archivoVacio_lanzaExcepcionYNoProcesaImagenes() {
        ModificarPublicacionRequest request = request("Editorial", List.of(10L));
        request.setArchivos(List.of(new MockMultipartFile(
                "archivos", "vacio.jpg", "image/jpeg", new byte[0])));

        assertThrows(org.mgroko.backend.storage.exception.ArchivoVacioException.class,
                () -> servicio.modificar(1L, 7L, 99L, request));

        verify(storageService, never()).almacenarImagenPublicacion(any());
    }

    @Test
    void modificar_respuestaIncluyeImagenesYNoExponeContadores() {
        PublicacionResponse response = servicio.modificar(
                1L, 7L, 99L, request("Editorial", List.of(10L, 20L)));

        assertTrue(response.cantidadMeGusta() == 0);
        assertTrue(response.cantidadComentarios() == 0);
        assertEquals(2, response.imagenes().size());
    }
}
