package org.mgroko.backend.auditoria.dto;

/**
 * Detalle campo a campo de un evento de auditoría.
 *
 * <p>{@code anterior} y {@code nuevo} se guardan tal cual los recibe el
 * helper de base de datos. Un valor {@code null} representa ausencia de dato
 * anterior o nuevo, no una cadena vacía.</p>
 */
public record AuditoriaDetalleRequest(
        String campo,
        String anterior,
        String nuevo
) {
}
