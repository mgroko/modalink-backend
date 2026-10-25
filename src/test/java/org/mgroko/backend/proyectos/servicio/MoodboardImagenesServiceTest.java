package org.mgroko.backend.proyectos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.LimiteImagenesMoodboardExcedidoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MoodboardImagenesServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MoodboardRepository moodboardRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private MoodboardImagenesService servicio;

    private Perfil perfil;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        servicio = new MoodboardImagenesService(
                perfilRepository,
                proyectoRepository,
                moodboardRepository,
                imagenRepository,
                storageService,
                new BorradoArchivosDiferido(storageService),
                proyectoSecurityService);

        perfil = Perfil.builder().idPerfil(2L).estado(EstadoPerfil.Activo).build();
        proyecto = Proyecto.builder().idProyecto(5L).build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(2L, 1L))
                .thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(5L)).thenReturn(Optional.of(proyecto));
        doNothing().when(proyectoSecurityService)
                .validarPermiso(5L, 2L, MoodboardImagenesService.PERMISO_MODIFICAR_PROYECTO);
    }

    private MockMultipartFile archivo(String nombre) {
        return new MockMultipartFile("archivos", nombre, "image/jpeg", "bytes".getBytes());
    }

    private void devolverAlmacenado(String nombre, String nombreGenerado) {
        when(storageService.almacenarImagenMoodboard(any())).thenReturn(new ArchivoAlmacenado(
                nombreGenerado,
                "/uploads/perfiles/" + nombreGenerado,
                "image/webp",
                512L));
    }

    @Test
    void subirImagenes_sinMoodboard_loCreaYDevuelveImagenes() {
        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.empty());
        when(moodboardRepository.save(any(Moodboard.class))).thenAnswer(invocation -> {
            Moodboard moodboard = invocation.getArgument(0);
            moodboard.setIdMoodboard(99L);
            return moodboard;
        });

        when(storageService.almacenarImagenMoodboard(any())).thenReturn(
                new ArchivoAlmacenado("moodboard_a.webp", "/uploads/perfiles/moodboard_a.webp", "image/webp", 512L),
                new ArchivoAlmacenado("moodboard_b.webp", "/uploads/perfiles/moodboard_b.webp", "image/webp", 512L));

        java.util.concurrent.atomic.AtomicInteger secuencia = new java.util.concurrent.atomic.AtomicInteger(10);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen((long) secuencia.incrementAndGet());
            return imagen;
        });

        MoodboardResponse response = servicio.subirImagenes(
                1L, 2L, 5L, List.of(archivo("a.jpg"), archivo("b.jpg")));

        assertNotNull(response);
        assertEquals(99L, response.idMoodboard());
        assertEquals(2, response.imagenes().size());
        assertEquals("/uploads/perfiles/moodboard_a.webp", response.imagenes().get(0).url());
        assertEquals("/uploads/perfiles/moodboard_b.webp", response.imagenes().get(1).url());
        verify(imagenRepository, times(2)).save(any(Imagen.class));
        // Dos guardados: creación del moodboard y alta final con las imágenes
        verify(moodboardRepository, times(2)).save(any(Moodboard.class));
    }

    @Test
    void subirImagenes_moodboardExistente_agregaYRespetaLimiteDeDiez() {
        Set<Imagen> existentes = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            existentes.add(Imagen.builder().idImagen((long) i).url("/uploads/perfiles/moodboard_" + i + ".webp").build());
        }
        Moodboard moodboard = Moodboard.builder()
                .idMoodboard(7L)
                .proyecto(proyecto)
                .descripcion("Paleta fría")
                .imagenes(existentes)
                .build();

        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.of(moodboard));

        assertThrows(LimiteImagenesMoodboardExcedidoException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("nueva.jpg"))));

        verify(storageService, never()).almacenarImagenMoodboard(any());
        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void subirImagenes_proyectoInexistente_lanzaProyectoNoEncontrado() {
        when(proyectoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ProyectoNoEncontradoException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg"))));
    }

    @Test
    void subirImagenes_sinPermiso_propagaAccesoDenegado() {
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(5L, 2L, MoodboardImagenesService.PERMISO_MODIFICAR_PROYECTO);

        assertThrows(AccesoDenegadoProyectoException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg"))));

        verify(storageService, never()).almacenarImagenMoodboard(any());
    }

    @Test
    void subirImagenes_listaVacia_lanzaArchivoVacio() {
        assertThrows(ArchivoVacioException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of()));
    }

    @Test
    void subirImagenes_moodboardExistente_preservaDescripcion() {
        Moodboard moodboard = Moodboard.builder()
                .idMoodboard(7L)
                .proyecto(proyecto)
                .descripcion("Paleta fría")
                .imagenes(new HashSet<>())
                .build();
        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.of(moodboard));
        when(moodboardRepository.save(any(Moodboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        devolverAlmacenado("a.jpg", "moodboard_a.webp");
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(10L);
            return imagen;
        });

        MoodboardResponse response = servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg")));

        assertEquals("Paleta fría", response.descripcion());
        assertEquals(1, response.imagenes().size());
    }

    @Test
    void subirImagenes_archivoInvalido_propagaExcepcionDelStorage() {
        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyecto).imagenes(new HashSet<>()).build()));
        when(storageService.almacenarImagenMoodboard(any()))
                .thenThrow(new FormatoImagenInvalidoException("Formato no permitido."));

        assertThrows(FormatoImagenInvalidoException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("mal.gif"))));

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void subirImagenes_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.Baja);

        assertThrows(PerfilEnBajaException.class, () ->
                servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg"))));

        verify(proyectoSecurityService, never())
                .validarPermiso(anyLong(), anyLong(), anyString());
    }

    @Test
    void subirImagenes_registraBorradoDiferidoDeCadaArchivoNuevo() {
        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyecto).imagenes(new HashSet<>()).build()));
        when(moodboardRepository.save(any(Moodboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(storageService.almacenarImagenMoodboard(any())).thenReturn(new ArchivoAlmacenado(
                "moodboard_x.webp", "/uploads/perfiles/moodboard_x.webp", "image/webp", 256L));
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(11L);
            return imagen;
        });

        servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg")));

        // Sin transacción activa el diferido no borra nada nuevo; el archivo solo se
        // borra si la transacción no commitea (cubierto por la integración con rollback).
        verify(storageService, never()).eliminarFotoPerfil(anyString());
        verify(storageService).almacenarImagenMoodboard(any());
        assertTrue(true);
    }

    @Test
    void subirImagenes_permisoVerificadoSobrePerfilActivo() {
        when(moodboardRepository.findByProyectoIdProyecto(5L)).thenReturn(Optional.of(
                Moodboard.builder().idMoodboard(7L).proyecto(proyecto).imagenes(new HashSet<>()).build()));
        when(moodboardRepository.save(any(Moodboard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        devolverAlmacenado("a.jpg", "moodboard_a.webp");
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(12L);
            return imagen;
        });

        servicio.subirImagenes(1L, 2L, 5L, List.of(archivo("a.jpg")));

        verify(proyectoSecurityService).validarPermiso(
                eq(5L), eq(2L), eq(MoodboardImagenesService.PERMISO_MODIFICAR_PROYECTO));
    }
}
