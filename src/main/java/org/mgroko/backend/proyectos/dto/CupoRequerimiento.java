package org.mgroko.backend.proyectos.dto;

/**
 * Estado de cobertura de un requerimiento de personal.
 *
 * <p>{@code cubiertos} cuenta miembros o asignaciones activas vinculadas al
 * requerimiento, no postulaciones aceptadas. Una baja reduce el conteo y
 * vuelve a dejar faltantes.</p>
 */
public record CupoRequerimiento(
        Integer cantidad,
        long cubiertos,
        int faltantes,
        boolean satisfecho) {
}
