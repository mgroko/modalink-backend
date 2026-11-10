package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Configuración actual del scheduler de limpieza de imágenes.
 *
 * @param intervaloHoras    cada cuántas horas se ejecuta la limpieza
 * @param graciaMinutos     minutos de gracia antes de considerar un archivo huérfano
 * @param proximaEjecucion  próxima ejecución programada (aproximada si no hubo
 *                          una ejecución programada previa)
 * @param ultimaEjecucion   última ejecución, programada o manual; {@code null}
 *                          si todavía no se ejecutó
 */
public record ConfiguracionSchedulerLimpiezaImagenesResponse(
        Integer intervaloHoras,
        Integer graciaMinutos,
        LocalDateTime proximaEjecucion,
        LocalDateTime ultimaEjecucion
) {
}
