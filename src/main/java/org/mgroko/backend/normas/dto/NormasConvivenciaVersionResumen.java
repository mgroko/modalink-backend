package org.mgroko.backend.normas.dto;

/**
 * Resumen de una versión de las normas de convivencia para el historial.
 */
public record NormasConvivenciaVersionResumen(
        String version,
        String vigenteDesde,
        String vigenteHasta,
        String cambios) {
}
