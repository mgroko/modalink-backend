package org.mgroko.backend.storage.servicio;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.dto.ArchivoEnDisco;
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
     * Procesa, valida y almacena una imagen del moodboard en el formato de salida.
     *
     * @param archivo archivo multipart recibido
     * @return detalles del archivo almacenado
     */
    ArchivoAlmacenado almacenarImagenMoodboard(MultipartFile archivo);

    /**
     * Procesa, valida y almacena una imagen de publicación respetando su
     * relación de aspecto.
     *
     * @param archivo archivo multipart recibido
     * @return detalles del archivo almacenado
     */
    ArchivoAlmacenado almacenarImagenPublicacion(MultipartFile archivo);

    /**
     * Procesa, valida y almacena una imagen de polaroid respetando su relación
     * de aspecto.
     *
     * @param archivo archivo multipart recibido
     * @return detalles del archivo almacenado
     */
    ArchivoAlmacenado almacenarImagenPolaroid(MultipartFile archivo);

    /**
     * Elimina el archivo de almacenamiento según su nombre, buscándolo en todos
     * los directorios gestionados (perfiles, moodboard, publicaciones y
     * polaroids).
     *
     * @param nombreArchivo nombre del archivo a eliminar
     * @throws org.mgroko.backend.storage.exception.ErrorAlmacenamientoException
     *         si el archivo no se pudo borrar
     */
    void eliminarFotoPerfil(String nombreArchivo);

    /**
     * Elimina un archivo de un subdirectorio concreto.
     *
     * @param subdirectorio carpeta relativa dentro de {@code uploads}
     * @param nombreArchivo nombre simple del archivo
     * @throws org.mgroko.backend.storage.exception.ErrorAlmacenamientoException
     *         si el subdirectorio no existe o el archivo no se pudo borrar
     */
    void eliminarArchivo(String subdirectorio, String nombreArchivo);

    /**
     * Lista las fotos de perfil que existen en disco con su última modificación.
     * Solo se devuelven nombres simples de archivo del directorio de perfiles
     * (mismo criterio que {@code getFileName()} de {@code eliminarFotoPerfil}):
     * nunca rutas con separadores ni referencias a directorios padre.
     *
     * @return nombre de archivo - instante de última modificación
     */
    Map<String, Instant> listarFotografiasDePerfil();

    /**
     * Lista todos los archivos físicos de los directorios gestionados, con su
     * subdirectorio y fecha de modificación. Es la fuente que usa el job de
     * limpieza para reconciliar disco y base de datos.
     *
     * @return archivos encontrados en perfiles, moodboard, publicaciones y polaroids
     */
    List<ArchivoEnDisco> listarArchivos();
}
