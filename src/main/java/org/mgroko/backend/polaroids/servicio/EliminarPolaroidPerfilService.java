package org.mgroko.backend.polaroids.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.polaroids.exception.PolaroidNoEncontradaException;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarPolaroidPerfilService {

    private final PolaroidDatosService polaroidDatosService;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final ImagenRepository imagenRepository;
    private final PolaroidOrdenService polaroidOrdenService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public EliminarPolaroidPerfilService(
            PolaroidDatosService polaroidDatosService,
            PolaroidPerfilRepository polaroidPerfilRepository,
            ImagenRepository imagenRepository,
            PolaroidOrdenService polaroidOrdenService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.polaroidDatosService = polaroidDatosService;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.imagenRepository = imagenRepository;
        this.polaroidOrdenService = polaroidOrdenService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public void eliminar(Long idUsuario, Long idPerfilActivo, Long idPerfil, Long idPolaroid) {
        polaroidDatosService.bloquearPerfilPropioModelo(idUsuario, idPerfil, idPerfilActivo);

        PolaroidPerfil polaroid = polaroidPerfilRepository
                .findByIdPolaroidAndPerfilIdPerfil(idPolaroid, idPerfil)
                .orElseThrow(() -> new PolaroidNoEncontradaException(idPolaroid));

        Imagen imagen = polaroid.getImagen();

        polaroidPerfilRepository.delete(polaroid);
        polaroidPerfilRepository.flush();

        if (imagen != null) {
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.delete(imagen);
        }

        List<PolaroidPerfil> restantes = polaroidPerfilRepository
                .findByPerfilIdPerfilOrderByOrdenAsc(idPerfil);
        if (!restantes.isEmpty()) {
            polaroidOrdenService.reordenar(
                    restantes,
                    restantes.stream().map(PolaroidPerfil::getIdPolaroid).toList());
        }
    }
}
