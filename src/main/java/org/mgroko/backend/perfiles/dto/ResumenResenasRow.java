package org.mgroko.backend.perfiles.dto;

/**
 * Proyección de la consulta agregada de reseñas por perfil receptor.
 */
public record ResumenResenasRow(
        Long total,
        Double promedio,
        Long cinco,
        Long cuatro,
        Long tres,
        Long dos,
        Long uno) {
}
