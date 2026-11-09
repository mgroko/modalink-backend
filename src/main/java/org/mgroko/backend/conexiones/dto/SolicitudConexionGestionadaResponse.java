package org.mgroko.backend.conexiones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Resultado de aceptar o rechazar una solicitud de conexión (UC-45).
 */
public record SolicitudConexionGestionadaResponse(
        Long idSolicitudConexion,
        EstadoSolicitud estado,
        LocalDateTime fechaRespuesta,
        Long idConexion
) {
}
