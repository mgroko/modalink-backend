package org.mgroko.backend.storage.servicio;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Procesa, valida, optimiza (a 600x600 px) y almacena la foto de perfil.
     *
     * @param archivo archivo multipart recibido
     * @return detalles del archivo almacenado
     */
    ArchivoAlmacenado almacenarFotoPerfil(MultipartFile archivo);

    /**
     * Elimina el archivo de almacenamiento según su nombre de archivo en disco.
     *
     * @param nombreArchivo nombre del archivo a eliminar
     */
    void eliminarFotoPerfil(String nombreArchivo);
}
