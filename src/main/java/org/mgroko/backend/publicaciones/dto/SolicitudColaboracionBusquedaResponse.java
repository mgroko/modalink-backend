package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Ítem de UC-65. Incluye la publicación asociada y la dirección respecto del
 * perfil activo.
 */
public record SolicitudColaboracionBusquedaResponse(
        Long idSolicitudColaboracion,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaRespuesta,
        DireccionSolicitudColaboracion direccion,
        Long idPerfilRemitente,
        Long idPerfilDestinatario,
        AutorPublicacionResponse remitente,
        AutorPublicacionResponse destinatario,
        PublicacionSolicitudColaboracionResponse publicacion,
        boolean puedeAceptar,
        boolean puedeRechazar,
        boolean puedeEliminar
) {
}
