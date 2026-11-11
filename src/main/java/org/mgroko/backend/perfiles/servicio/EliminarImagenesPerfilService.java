package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto único de liberación de las imágenes de un perfil cuando el perfil pasa
 * a {@code BAJA}: desvincula y borra las filas de {@code imagen} dentro de la
 * transacción y deja el borrado de los archivos físicos para después del
 * commit.
 * <p>
 * Incluye la foto de perfil y las polaroids. Las publicaciones y el moodboard
 * pertenecen a otros agregados y no se eliminan desde acá.
 * <p>
 * Los perfiles en {@code PENDIENTE_BAJA} conservan sus imágenes: este método
 * solo se invoca en la transición efectiva a {@code BAJA}.
 */
@Service
public class EliminarImagenesPerfilService {

    private final PerfilRepository perfilRepository;
    private final ImagenRepository imagenRepository;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public EliminarImagenesPerfilService(
            PerfilRepository perfilRepository,
            ImagenRepository imagenRepository,
            PolaroidPerfilRepository polaroidPerfilRepository,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.perfilRepository = perfilRepository;
        this.imagenRepository = imagenRepository;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    /**
     * Elimina las imágenes del perfil: filas de {@code imagen} en la transacción
     * en curso y archivos físicos después del commit.
     *
     * @param perfil perfil recién pasado a {@code BAJA} (debe estar gestionado
     *               por la transacción del llamador)
     */
    @Transactional
    public void eliminarImagenesDePerfil(Perfil perfil) {
        eliminarFotoPerfil(perfil);
        eliminarPolaroids(perfil);
    }

    private void eliminarFotoPerfil(Perfil perfil) {
        Imagen imagen = perfil.getImagen();
        if (imagen == null) {
            return;
        }

        String nombreArchivo = imagen.getNombreArchivo();

        perfil.setImagen(null);
        perfilRepository.save(perfil);
        imagenRepository.delete(imagen);

        borradoArchivosDiferido.registrarBorrados(null, nombreArchivo);
    }

    private void eliminarPolaroids(Perfil perfil) {
        List<PolaroidPerfil> polaroids = polaroidPerfilRepository
                .findByPerfilIdPerfilOrderByOrdenAsc(perfil.getIdPerfil());

        if (polaroids.isEmpty()) {
            return;
        }

        for (PolaroidPerfil polaroid : polaroids) {
            polaroidPerfilRepository.delete(polaroid);
        }
        polaroidPerfilRepository.flush();

        for (PolaroidPerfil polaroid : polaroids) {
            Imagen imagen = polaroid.getImagen();
            if (imagen == null) {
                continue;
            }
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.delete(imagen);
        }
    }

    /**
     * Variante para el job de limpieza: carga el perfil en su propia
     * transacción y elimina sus imágenes.
     *
     * @param idPerfil identificador del perfil
     */
    @Transactional
    public void eliminarImagenesDePerfil(Long idPerfil) {
        perfilRepository.findById(idPerfil).ifPresent(this::eliminarImagenesDePerfil);
    }
}
