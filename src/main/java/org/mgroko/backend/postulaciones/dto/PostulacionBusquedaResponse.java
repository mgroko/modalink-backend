package org.mgroko.backend.postulaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Ítem de UC-74. Incluye los datos del postulante, del requerimiento y el
 * estado de cupo derivado de miembros/asignaciones activos.
 */
public record PostulacionBusquedaResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        LocalDateTime fechaPostulacion,
        Long idPerfil,
        String nombreArtistico,
        Long idProfesion,
        String nombreProfesion,
        Long idRequerimiento,
        String descripcionRequerimiento,
        Long idActividad,
        String nombreActividad,
        Integer cantidadRequerida,
        Long cubiertos,
        Integer faltantes,
        boolean satisfecho,
        boolean aportaCupo,
        boolean puedeAceptar,
        boolean puedeRechazar) {
}
