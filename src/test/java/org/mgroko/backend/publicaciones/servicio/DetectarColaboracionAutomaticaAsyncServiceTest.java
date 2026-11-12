package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DetectarColaboracionAutomaticaAsyncServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private DetectarColaboracionAutomaticaService detectorService;

    private DetectarColaboracionAutomaticaAsyncService servicio;

    @BeforeEach
    void setUp() {
        servicio = new DetectarColaboracionAutomaticaAsyncService(
                publicacionRepository,
                imagenRepository,
                detectorService);
    }

    @Test
    void procesar_recargaPublicacionEImagenesYDelegaAlDetector() {
        Imagen imagen = Imagen.builder().idImagen(10L).build();
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .imagenes(List.of(imagen))
                .build();

        when(publicacionRepository.findById(99L)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findAllById(List.of(10L))).thenReturn(List.of(imagen));

        servicio.procesar(99L, List.of(10L));

        verify(detectorService).detectar(publicacion, List.of(imagen));
    }

    @Test
    void procesar_publicacionInexistente_noDelega() {
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        servicio.procesar(99L, List.of(10L));

        verify(detectorService, never()).detectar(any(), any());
    }

    @Test
    void procesar_sinImagenes_noDelega() {
        Publicacion publicacion = Publicacion.builder().idPublicacion(99L).build();

        when(publicacionRepository.findById(99L)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findAllById(List.of(10L))).thenReturn(List.of());

        servicio.procesar(99L, List.of(10L));

        verify(detectorService, never()).detectar(any(), any());
    }

    @Test
    void procesar_errorDelDetector_noPropaga() {
        Imagen imagen = Imagen.builder().idImagen(10L).build();
        Publicacion publicacion = Publicacion.builder()
                .idPublicacion(99L)
                .imagenes(List.of(imagen))
                .build();

        when(publicacionRepository.findById(99L)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findAllById(List.of(10L))).thenReturn(List.of(imagen));
        doThrow(new IllegalStateException("boom"))
                .when(detectorService).detectar(publicacion, List.of(imagen));

        assertDoesNotThrow(() -> servicio.procesar(99L, List.of(10L)));
    }

    @Test
    void procesar_sinIds_noConsultaRepositorios() {
        servicio.procesar(null, List.of(10L));
        servicio.procesar(99L, List.of());

        verify(publicacionRepository, never()).findById(any());
        verify(imagenRepository, never()).findAllById(any());
    }
}
