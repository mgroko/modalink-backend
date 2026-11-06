package org.mgroko.backend.actividades.dto;

/**
 * Miembro activo que participa únicamente de la actividad que se quiere
 * eliminar. El frontend debe pedir una decisión por cada elemento de esta
 * lista.
 */
public record MiembroExclusivoResponse(
        Long idAsignacionActividad,
        Long idMiembro,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        Long idProfesion,
        String nombreProfesion,
        boolean puedeEliminar,
        String motivoNoEliminable
) {
}
