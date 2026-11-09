package org.mgroko.backend.conexiones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Ítem de UC-46. La contraparte es el remitente o el destinatario según la
 * dirección de la solicitud respecto del perfil activo.
 */
public record SolicitudConexionBusquedaResponse(
        Long idSolicitudConexion,
        EstadoSolicitud estado,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaRespuesta,
        DireccionSolicitudConexion direccion,
        Long idPerfilRemitente,
        Long idPerfilDestinatario,
        PerfilConexionResponse perfil,
        boolean puedeAceptar,
        boolean puedeRechazar,
        boolean puedeCancelar
) {
}
