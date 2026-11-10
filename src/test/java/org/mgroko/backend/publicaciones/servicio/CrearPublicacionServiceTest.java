package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.CrearPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.LimiteImagenesPublicacionExcedidoException;
import org.mgroko.backend.publicaciones.exception.ProyectoNoAsociableException;
import org.mgroko.backend.publicaciones.exception.PublicacionInvalidaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearPublicacionServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private StorageService storageService;

    @Mock
    private BorradoArchivosDiferido borradoArchivosDiferido;

    private CrearPublicacionService servicio;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        servicio = new CrearPublicacionService(
                perfilRepository,
                proyectoRepository,
                publicacionRepository,
                imagenRepository,
                proyectoVisibilidadService,
                ubicacionService,
                storageService,
                borradoArchivosDiferido);

        perfil = Perfil.builder()
                .idPerfil(7L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Diseñadora").build())
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(7L, 1L))
                .thenReturn(Optional.of(perfil));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocation -> {
            Publicacion publicacion = invocation.getArgument(0);
            if (publicacion.getIdPublicacion() == null) {
                publicacion.setIdPublicacion(99L);
            }
            return publicacion;
        });
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(10L);
            return imagen;
        });
        when(storageService.almacenarImagenPublicacion(any()))
                .thenReturn(new ArchivoAlmacenado(
                        "publicacion_a.webp",
                        "/uploads/publicaciones/publicacion_a.webp",
                        "image/webp",
                        100L));
        when(proyectoVisibilidadService.puedeVer(any(), anyLong())).thenReturn(true);
    }

    private CrearPublicacionRequest request(String titulo) {
        CrearPublicacionRequest request = new CrearPublicacionRequest();
        request.setTitulo(titulo);
        request.setArchivos(List.of(archivo("foto.jpg")));
        return request;
    }

    private MockMultipartFile archivo(String nombre) {
        return new MockMultipartFile("archivos", nombre, "image/jpeg", "bytes".getBytes());
    }

    @Test
    void crear_sinProyectoNiUbicacion_persistePublicacionEImagenes() {
        PublicacionResponse response = servicio.crear(1L, 7L, request("Editorial"));

        assertEquals(99L, response.idPublicacion());
        assertEquals("Editorial", response.titulo());
        assertEquals("ACTIVA", response.estado());
        assertEquals(1, response.imagenes().size());
        assertTrue(response.proyecto() == null);
        verify(borradoArchivosDiferido).registrarBorrados("publicacion_a.webp", null);
    }

    @Test
    void crear_conProyectoConfirmadoYMiembroActivo_loAsocia() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        when(proyectoRepository.findById(25L)).thenReturn(Optional.of(proyecto));
        when(proyectoVisibilidadService.esMiembroActivo(25L, 7L)).thenReturn(true);

        CrearPublicacionRequest request = request("Editorial");
        request.setIdProyecto(25L);

        PublicacionResponse response = servicio.crear(1L, 7L, request);

        assertNotNull(response.proyecto());
        assertEquals(25L, response.proyecto().idProyecto());
        assertTrue(response.proyecto().puedeVerProyecto());
    }

    @Test
    void crear_conProyectoFinalizado_loPermite() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.FINALIZADO)
                .build();
        when(proyectoRepository.findById(25L)).thenReturn(Optional.of(proyecto));
        when(proyectoVisibilidadService.esMiembroActivo(25L, 7L)).thenReturn(true);

        CrearPublicacionRequest request = request("Editorial");
        request.setIdProyecto(25L);

        assertNotNull(servicio.crear(1L, 7L, request).proyecto());
    }

    @Test
    void crear_sinPerfilActivo_lanzaExcepcion() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.crear(1L, null, request("Editorial")));
    }

    @Test
    void crear_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.crear(1L, 7L, request("Editorial")));
    }

    @Test
    void crear_tituloVacio_lanzaExcepcion() {
        assertThrows(PublicacionInvalidaException.class,
                () -> servicio.crear(1L, 7L, request("  ")));
    }

    @Test
    void crear_sinArchivos_lanzaExcepcion() {
        CrearPublicacionRequest request = request("Editorial");
        request.setArchivos(List.of());

        assertThrows(ArchivoVacioException.class, () -> servicio.crear(1L, 7L, request));
    }

    @Test
    void crear_masDeDiezArchivos_lanzaExcepcion() {
        CrearPublicacionRequest request = request("Editorial");
        request.setArchivos(java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> (MultipartFile) archivo("foto" + i + ".jpg"))
                .toList());

        assertThrows(LimiteImagenesPublicacionExcedidoException.class,
                () -> servicio.crear(1L, 7L, request));
    }

    @Test
    void crear_proyectoInexistente_lanzaExcepcion() {
        when(proyectoRepository.findById(25L)).thenReturn(Optional.empty());
        CrearPublicacionRequest request = request("Editorial");
        request.setIdProyecto(25L);

        assertThrows(ProyectoNoEncontradoException.class, () -> servicio.crear(1L, 7L, request));
    }

    @Test
    void crear_proyectoEnBorrador_lanzaProyectoNoAsociable() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.BORRADOR)
                .build();
        when(proyectoRepository.findById(25L)).thenReturn(Optional.of(proyecto));
        CrearPublicacionRequest request = request("Editorial");
        request.setIdProyecto(25L);

        assertThrows(ProyectoNoAsociableException.class, () -> servicio.crear(1L, 7L, request));
    }

    @Test
    void crear_perfilNoMiembro_lanzaProyectoNoAsociable() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(25L)
                .nombre("Campaña")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();
        when(proyectoRepository.findById(25L)).thenReturn(Optional.of(proyecto));
        when(proyectoVisibilidadService.esMiembroActivo(25L, 7L)).thenReturn(false);
        CrearPublicacionRequest request = request("Editorial");
        request.setIdProyecto(25L);

        assertThrows(ProyectoNoAsociableException.class, () -> servicio.crear(1L, 7L, request));
    }

    @Test
    void crear_archivoVacio_lanzaExcepcionYNoProcesaNada() {
        CrearPublicacionRequest request = request("Editorial");
        request.setArchivos(List.of(new MockMultipartFile(
                "archivos", "vacio.jpg", "image/jpeg", new byte[0])));

        assertThrows(ArchivoVacioException.class, () -> servicio.crear(1L, 7L, request));
        verify(storageService, never()).almacenarImagenPublicacion(any());
    }
}
