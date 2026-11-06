package org.mgroko.backend.postulaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Postulación emitida por el perfil activo, sin importar si es a un
 * requerimiento general del proyecto o a un requerimiento de actividad.
 *
 * <p>Se usa en el listado de UC-27 para que el usuario pueda identificar la
 * postulación que quiere dar de baja. {@code puedeDarseDeBaja} refleja la
 * precondición del caso de uso: sólo las postulaciones {@code PENDIENTE} se
 * pueden eliminar.</p>
 */
public record MiPostulacionResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        LocalDateTime fechaPostulacion,
        Long idProyecto,
        String nombreProyecto,
        Long idRequerimiento,
        Integer cantidad,
        String nombreProfesion,
        String descripcionRequerimiento,
        Long idActividad,
        String nombreActividad,
        boolean puedeDarseDeBaja
) {}
