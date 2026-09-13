package org.mgroko.backend.storage.dto;

public record ArchivoAlmacenado(
        String nombreArchivo,
        String url,
        String tipoMime,
        long tamanoBytes
) {}
