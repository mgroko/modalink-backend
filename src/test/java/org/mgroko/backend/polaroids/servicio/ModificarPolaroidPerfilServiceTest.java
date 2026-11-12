package org.mgroko.backend.polaroids.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.ModificarPolaroidRequest;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.TipoPolaroidResponse;
import org.mgroko.backend.polaroids.exception.PolaroidInvalidaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModificarPolaroidPerfilServiceTest {

    @Mock
    private PolaroidDatosService polaroidDatosService;

    @Mock
    private PolaroidCatalogoService polaroidCatalogoService;

    @Mock
    private PolaroidPerfilRepository polaroidPerfilRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private BorradoArchivosDiferido borradoArchivosDiferido;

    @Test
    void cambiarSoloElTipoNoActualizaLaFecha() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 10, 1, 10, 0);
        PolaroidPerfil polaroid = polaroid(fechaOriginal);
        TipoPolaroid nuevoTipo = TipoPolaroid.builder()
                .idTipoPolaroid(3L).codigo("PRIMER_PLANO_ESPALDAS").nombre("De espaldas").build();

        when(polaroidDatosService.bloquearPerfilPropioModelo(1L, 7L, 7L))
                .thenReturn(Perfil.builder().idPerfil(7L).build());
        when(polaroidDatosService.hayArchivo(any())).thenReturn(false);
        when(polaroidPerfilRepository.findByIdPolaroidAndPerfilIdPerfil(10L, 7L))
                .thenReturn(Optional.of(polaroid));
        when(polaroidDatosService.resolverTipo(3L)).thenReturn(nuevoTipo);
        when(polaroidPerfilRepository.save(polaroid)).thenReturn(polaroid);
        when(polaroidCatalogoService.toResponse(nuevoTipo))
                .thenReturn(new TipoPolaroidResponse(3L, "PRIMER_PLANO_ESPALDAS", "De espaldas", "Primer plano · De espaldas"));

        ModificarPolaroidRequest request = new ModificarPolaroidRequest();
        request.setIdTipoPolaroid(3L);

        ModificarPolaroidPerfilService servicio = servicio();
        PolaroidResponse response = servicio.modificar(1L, 7L, 7L, 10L, request);

        assertEquals(fechaOriginal, response.fechaRegistro());
        assertEquals(3L, response.tipo().idTipoPolaroid());
        verify(storageService, never()).almacenarImagenPolaroid(any());
    }

    @Test
    void reemplazarLaImagenActualizaLaFechaYBorraLaAnterior() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 10, 1, 10, 0);
        PolaroidPerfil polaroid = polaroid(fechaOriginal);
        Imagen anterior = polaroid.getImagen();
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "nueva.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(polaroidDatosService.bloquearPerfilPropioModelo(1L, 7L, 7L))
                .thenReturn(Perfil.builder().idPerfil(7L).build());
        when(polaroidDatosService.hayArchivo(archivo)).thenReturn(true);
        when(polaroidPerfilRepository.findByIdPolaroidAndPerfilIdPerfil(10L, 7L))
                .thenReturn(Optional.of(polaroid));
        when(storageService.almacenarImagenPolaroid(archivo))
                .thenReturn(new ArchivoAlmacenado("polaroid_nueva.webp", "/uploads/polaroids/polaroid_nueva.webp", "image/webp", 200));
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(inv -> {
            Imagen imagen = inv.getArgument(0);
            imagen.setIdImagen(21L);
            return imagen;
        });
        when(polaroidPerfilRepository.save(polaroid)).thenReturn(polaroid);
        when(polaroidCatalogoService.toResponse(polaroid.getTipoPolaroid()))
                .thenReturn(new TipoPolaroidResponse(2L, "PRIMER_PLANO_PERFIL", "De perfil", "Primer plano · De perfil"));

        ModificarPolaroidRequest request = new ModificarPolaroidRequest();
        request.setArchivo(archivo);

        PolaroidResponse response = servicio().modificar(1L, 7L, 7L, 10L, request);

        assertEquals(21L, response.idImagen());
        assertEquals(true, response.fechaRegistro().isAfter(fechaOriginal));
        verify(borradoArchivosDiferido).registrarBorrados("polaroid_nueva.webp", "polaroid_vieja.webp");
        verify(imagenRepository).delete(anterior);
    }

    @Test
    void exigeAlMenosUnCambio() {
        when(polaroidDatosService.bloquearPerfilPropioModelo(1L, 7L, 7L))
                .thenReturn(Perfil.builder().idPerfil(7L).build());

        assertThrows(PolaroidInvalidaException.class,
                () -> servicio().modificar(1L, 7L, 7L, 10L, new ModificarPolaroidRequest()));
    }

    private PolaroidPerfil polaroid(LocalDateTime fecha) {
        Imagen imagen = Imagen.builder()
                .idImagen(20L)
                .nombreArchivo("polaroid_vieja.webp")
                .url("/uploads/polaroids/polaroid_vieja.webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();
        return PolaroidPerfil.builder()
                .idPolaroid(10L)
                .orden(1)
                .fechaRegistro(fecha)
                .imagen(imagen)
                .tipoPolaroid(TipoPolaroid.builder()
                        .idTipoPolaroid(2L)
                        .codigo("PRIMER_PLANO_PERFIL")
                        .nombre("De perfil")
                        .build())
                .build();
    }

    private ModificarPolaroidPerfilService servicio() {
        return new ModificarPolaroidPerfilService(
                polaroidDatosService, polaroidCatalogoService, polaroidPerfilRepository,
                imagenRepository, storageService, borradoArchivosDiferido);
    }
}
