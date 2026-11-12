package org.mgroko.backend.storage.dto;

public record ArchivoAlmacenado(
        String nombreArchivo,
        String url,
        String tipoMime,
        long tamanoBytes,
        String sha256Original,
        String sha256Contenido,
        Long phash
) {

    /**
     * Constructor de compatibilidad para consumidores que no necesitan
     * huellas (tests, almacenamientos históricos).
     */
    public ArchivoAlmacenado(
            String nombreArchivo,
            String url,
            String tipoMime,
            long tamanoBytes) {
        this(nombreArchivo, url, tipoMime, tamanoBytes, null, null, null);
    }
}
