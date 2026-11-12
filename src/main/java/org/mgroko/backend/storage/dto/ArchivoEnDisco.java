package org.mgroko.backend.storage.dto;

import java.time.Instant;

/**
 * Archivo físico encontrado por el job de limpieza.
 *
 * <p>Incluye el subdirectorio para poder borrar la ruta exacta y evitar
 * ambigüedades cuando el mismo nombre de archivo existiera en más de una
 * carpeta de almacenamiento.</p>
 *
 * @param subdirectorio     carpeta relativa dentro de {@code uploads}
 *                          (por ejemplo {@code perfiles}, {@code moodboard} o
 *                          {@code publicaciones})
 * @param nombreArchivo     nombre simple del archivo
 * @param fechaModificacion última modificación registrada en disco
 */
public record ArchivoEnDisco(
        String subdirectorio,
        String nombreArchivo,
        Instant fechaModificacion
) {
}
