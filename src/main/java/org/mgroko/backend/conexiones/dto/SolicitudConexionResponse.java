package org.mgroko.backend.conexiones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Salida de UC-43: la solicitud de conexión creada.
 */
public record SolicitudConexionResponse(
        Long idSolicitudConexion,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaRespuesta,
        PerfilConexionResponse remitente,
        PerfilConexionResponse destinatario
) {
}
