package org.mgroko.backend.storage.servicio;

import java.time.Instant;
import java.util.Map;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Procesa, valida y almacena la foto de perfil en el formato de salida.
     *
     * @param archivo archivo multipart recibido
     * @return detalles del archivo almacenado
     */
    ArchivoAlmacenado almacenarFotoPerfil(MultipartFile archivo);

    /**
     * Elimina el archivo de almacenamiento según su nombre de archivo en disco.
     *
     * @param nombreArchivo nombre del archivo a eliminar
     * @throws ErrorAlmacenamientoException si el archivo no se pudo borrar
     */
    void eliminarFotoPerfil(String nombreArchivo);

    /**
     * Lista las fotos de perfil que existen en disco con su última modificación.
     * Solo se devuelven nombres simples de archivo del directorio de perfiles
     * (mismo criterio que {@code getFileName()} de {@code eliminarFotoPerfil}):
     * nunca rutas con separadores ni referencias a directorios padre.
     *
     * @return nombre de archivo - instante de última modificación
     */
    Map<String, Instant> listarFotografiasDePerfil();
}
