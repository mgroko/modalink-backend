package org.mgroko.backend.proyectos.dto;

/**
 * Cupo de un requerimiento expuesto en la previsualización de confirmación.
 */
public record RequerimientoCupoResponse(
        Long idRequerimiento,
        Long idActividad,
        String nombreActividad,
        Long idProfesion,
        String nombreProfesion,
        Integer cantidad,
        long cubiertos,
        int faltantes,
        boolean satisfecho) {
}
