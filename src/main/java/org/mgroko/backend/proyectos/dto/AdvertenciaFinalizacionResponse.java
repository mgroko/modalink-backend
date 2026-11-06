package org.mgroko.backend.proyectos.dto;

/**
 * Advertencia de UC-37 que no impide finalizar, pero requiere confirmación
 * explícita. Ejemplo: finalizar antes de la fecha de entrega estipulada.
 */
public record AdvertenciaFinalizacionResponse(
        String codigo,
        String mensaje,
        String entidadTipo,
        Long entidadId) {
}
