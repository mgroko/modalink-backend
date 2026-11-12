package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Resultado de aceptar o rechazar una solicitud de colaboración (UC-62/63).
 */
public record SolicitudColaboracionGestionadaResponse(
        Long idSolicitudColaboracion,
        EstadoSolicitud estado,
        LocalDateTime fechaRespuesta,
        Long idPublicacion,
        String tituloPublicacion,
        boolean colaboradorAgregado
) {
}
