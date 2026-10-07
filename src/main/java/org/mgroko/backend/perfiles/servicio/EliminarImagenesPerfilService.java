package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto &uacute;nico de liberaci&oacute;n de las im&aacute;genes de un perfil cuando el
 * perfil pasa a {@code BAJA}: desvincula y borra la fila de {@code imagen} dentro
 * de la transacci&oacute;n y deja el borrado del archivo f&iacute;sico para despu&eacute;s
 * del commit (mismo criterio que la Fase 3).
 * <p>
 * Cuando existan las publicaciones independientes del perfil, su
 * eliminaci&oacute;n se agrega en {@link #eliminarImagenesDePerfil(Perfil)} para que
 * los llamadores (expiraci&oacute;n de perfiles y de cuentas) no cambien.
 * <p>
 * Los perfiles en {@code PENDIENTE_BAJA} conservan su foto: este m&eacute;todo solo se
 * invoca en la transici&oacute;n efectiva a {@code BAJA}.
 */
@Service
public class EliminarImagenesPerfilService {

    private final PerfilRepository perfilRepository;
    private final ImagenRepository imagenRepository;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public EliminarImagenesPerfilService(
            PerfilRepository perfilRepository,
            ImagenRepository imagenRepository,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.perfilRepository = perfilRepository;
        this.imagenRepository = imagenRepository;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    /**
     * Elimina las im&aacute;genes del perfil: fila de {@code imagen} en la transacci&oacute;n
     * en curso y archivo f&iacute;sico despu&eacute;s del commit.
     *
     * @param perfil perfil reci&eacute;n pasado a {@code BAJA} (debe estar gestionado
     *               por la transacci&oacute;n del llamador)
     */
    @Transactional
    public void eliminarImagenesDePerfil(Perfil perfil) {
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

    /**
     * Variante para el job de limpieza: carga el perfil en su propia
     * transacci&oacute;n y elimina sus im&aacute;genes.
     *
     * @param idPerfil identificador del perfil
     */
    @Transactional
    public void eliminarImagenesDePerfil(Long idPerfil) {
        perfilRepository.findById(idPerfil).ifPresent(this::eliminarImagenesDePerfil);
    }
}
