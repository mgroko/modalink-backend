package org.mgroko.backend.postulaciones.dto;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Resultado de aceptar o rechazar una postulación.
 */
public record PostulacionGestionadaResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        Long idProyecto,
        Long idRequerimiento,
        Long idPerfil,
        Long idMiembro,
        Long idAsignacionActividad) {
}
