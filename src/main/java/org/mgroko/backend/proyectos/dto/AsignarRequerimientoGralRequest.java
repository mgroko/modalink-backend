package org.mgroko.backend.proyectos.dto;

/**
 * Asignación, reasignación o desasignación de un miembro a un requerimiento
 * general. {@code idRequerimientoGral = null} desasigna.
 */
public record AsignarRequerimientoGralRequest(
        Long idRequerimientoGral
) {
}
