package org.mgroko.backend.mensajeria.dto;

/**
 * Datos básicos de un perfil dentro del chat.
 */
public record PerfilMensajeResponse(
        Long idPerfil,
        String nombreArtistico,
        String nombre,
        String apellido,
        Long idProfesion,
        String nombreProfesion,
        String fotoUrl
) {
}
