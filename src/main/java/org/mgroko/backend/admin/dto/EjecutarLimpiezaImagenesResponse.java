package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Resultado de una ejecución manual del job de limpieza de imágenes.
 */
public record EjecutarLimpiezaImagenesResponse(
        int registrosAfectados,
        int archivosHuerfanos,
        int imagenesHuerfanas,
        int imagenesPerfilesBaja,
        int errores,
        LocalDateTime ejecutadoEn,
        String mensaje
) {
}
