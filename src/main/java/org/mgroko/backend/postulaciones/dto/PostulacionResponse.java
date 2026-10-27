package org.mgroko.backend.postulaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Respuesta de alta de una postulación.
 *
 * @param idPostulacion    identificador de la postulación creada
 * @param tipo             requerimiento general o de actividad
 * @param estado           estado inicial de la postulación
 * @param fechaPostulacion fecha de alta
 * @param idProyecto       proyecto al que pertenece el requerimiento
 * @param idRequerimiento  requerimiento general o de actividad
 * @param tentativa        {@code true} si la disponibilidad se evaluó sobre
 *                         una fecha de inicio estimada y debe re-verificarse
 *                         al confirmar el proyecto (UC-60)
 */
public record PostulacionResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        LocalDateTime fechaPostulacion,
        Long idProyecto,
        Long idRequerimiento,
        boolean tentativa) {
}
