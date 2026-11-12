package org.mgroko.backend.polaroids.servicio;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.ModificarPolaroidRequest;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.exception.PolaroidInvalidaException;
import org.mgroko.backend.polaroids.exception.PolaroidNoEncontradaException;
import org.mgroko.backend.polaroids.mapper.PolaroidMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modifica una polaroid existente: puede cambiar solo el tipo, solo la imagen
 * o ambos. El orden no se toca; se gestiona con el reordenamiento.
 */
@Service
public class ModificarPolaroidPerfilService {

    private final PolaroidDatosService polaroidDatosService;
    private final PolaroidCatalogoService polaroidCatalogoService;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final ImagenRepository imagenRepository;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public ModificarPolaroidPerfilService(
            PolaroidDatosService polaroidDatosService,
            PolaroidCatalogoService polaroidCatalogoService,
            PolaroidPerfilRepository polaroidPerfilRepository,
            ImagenRepository imagenRepository,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.polaroidDatosService = polaroidDatosService;
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public PolaroidResponse modificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPerfil,
            Long idPolaroid,
            ModificarPolaroidRequest request) {

        polaroidDatosService.bloquearPerfilPropioModelo(idUsuario, idPerfil, idPerfilActivo);

        boolean cambiaTipo = request.getIdTipoPolaroid() != null;
        boolean cambiaImagen = polaroidDatosService.hayArchivo(request.getArchivo());

        if (!cambiaTipo && !cambiaImagen) {
            throw new PolaroidInvalidaException(
                    "Debés indicar una imagen nueva, un tipo de polaroid o ambos.");
        }

        PolaroidPerfil polaroid = polaroidPerfilRepository
                .findByIdPolaroidAndPerfilIdPerfil(idPolaroid, idPerfil)
                .orElseThrow(() -> new PolaroidNoEncontradaException(idPolaroid));

        if (cambiaTipo) {
            TipoPolaroid tipo = polaroidDatosService.resolverTipo(request.getIdTipoPolaroid());
            polaroid.setTipoPolaroid(tipo);
        }

        if (cambiaImagen) {
            reemplazarImagen(polaroid, request);
        }

        PolaroidPerfil guardada = polaroidPerfilRepository.save(polaroid);
        return PolaroidMapper.toResponse(guardada, polaroidCatalogoService);
    }

    private void reemplazarImagen(PolaroidPerfil polaroid, ModificarPolaroidRequest request) {
        Imagen imagenAnterior = polaroid.getImagen();
        ArchivoAlmacenado almacenado = storageService.almacenarImagenPolaroid(request.getArchivo());

        borradoArchivosDiferido.registrarBorrados(
                almacenado.nombreArchivo(),
                imagenAnterior != null ? imagenAnterior.getNombreArchivo() : null);

        Imagen imagenNueva = imagenRepository.save(Imagen.builder()
                .nombreArchivo(almacenado.nombreArchivo())
                .url(almacenado.url())
                .tipoImagen(almacenado.tipoMime())
                .tamanoBytes((int) almacenado.tamanoBytes())
                .estado("Activa")
                .sha256Original(almacenado.sha256Original())
                .sha256Contenido(almacenado.sha256Contenido())
                .phash(almacenado.phash())
                .phashVersion(org.mgroko.backend.storage.servicio.HuellaImagenService.PHASH_VERSION)
                .build());

        polaroid.setImagen(imagenNueva);
        polaroid.setFechaRegistro(LocalDateTime.now());

        // Se confirma la nueva referencia antes de borrar la fila anterior.
        polaroidPerfilRepository.saveAndFlush(polaroid);

        if (imagenAnterior != null) {
            imagenRepository.delete(imagenAnterior);
        }
    }
}
