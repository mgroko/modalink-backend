package org.mgroko.backend.polaroids.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.CrearPolaroidRequest;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.TipoPolaroidResponse;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearPolaroidPerfilServiceTest {

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
    void creaLaPolaroidAlFinalDelOrden() {
        Perfil perfil = Perfil.builder().idPerfil(7L).build();
        TipoPolaroid tipo = TipoPolaroid.builder().idTipoPolaroid(2L).codigo("PRIMER_PLANO_PERFIL").nombre("De perfil").build();

        when(polaroidDatosService.bloquearPerfilPropioModelo(1L, 7L, 7L)).thenReturn(perfil);
        when(polaroidPerfilRepository.countByPerfilIdPerfil(7L)).thenReturn(1L);
        when(polaroidPerfilRepository.obtenerMaximoOrden(7L)).thenReturn(1);
        when(polaroidDatosService.resolverTipo(2L)).thenReturn(tipo);
        when(storageService.almacenarImagenPolaroid(any(MultipartFile.class)))
                .thenReturn(new ArchivoAlmacenado("polaroid_1.webp", "/uploads/polaroids/polaroid_1.webp", "image/webp", 100));
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(inv -> {
            Imagen imagen = inv.getArgument(0);
            imagen.setIdImagen(20L);
            return imagen;
        });
        when(polaroidPerfilRepository.save(any(PolaroidPerfil.class))).thenAnswer(inv -> {
            PolaroidPerfil polaroid = inv.getArgument(0);
            polaroid.setIdPolaroid(10L);
            return polaroid;
        });
        when(polaroidCatalogoService.toResponse(tipo))
                .thenReturn(new TipoPolaroidResponse(2L, "PRIMER_PLANO_PERFIL", "De perfil", "Primer plano · De perfil"));

        CrearPolaroidRequest request = new CrearPolaroidRequest();
        request.setArchivo(new MockMultipartFile("archivo", "pola.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        request.setIdTipoPolaroid(2L);

        CrearPolaroidPerfilService servicio = new CrearPolaroidPerfilService(
                polaroidDatosService, polaroidCatalogoService, polaroidPerfilRepository,
                imagenRepository, storageService, borradoArchivosDiferido);

        PolaroidResponse response = servicio.crear(1L, 7L, 7L, request);

        assertEquals(10L, response.idPolaroid());
        assertEquals(2, response.orden());
        assertEquals("Primer plano · De perfil", response.tipo().nombreCompleto());
        verify(borradoArchivosDiferido).registrarBorrados("polaroid_1.webp", null);
    }
}
