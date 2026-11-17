package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Resumen de perfil para el listado administrativo.
 */
public record AdminPerfilResumenResponse(
        Long idPerfil,
        String nombreArtistico,
        String estado,
        String profesion,
        Long idUsuario,
        String nombreUsuario,
        String apellidoUsuario,
        String correo,
        LocalDateTime fechaSolicitudBaja,
        String fotoUrl
) {
}
