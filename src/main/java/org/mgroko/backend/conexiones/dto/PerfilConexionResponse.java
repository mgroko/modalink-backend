package org.mgroko.backend.conexiones.dto;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

/**
 * Datos mínimos de un perfil remitente o destinatario de una conexión.
 */
public record PerfilConexionResponse(
        Long idPerfil,
        String nombreArtistico,
        String nombre,
        String apellido,
        Long idProfesion,
        String nombreProfesion,
        String fotoUrl,
        CiudadResponse ciudad
) {
}
