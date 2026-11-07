package org.mgroko.backend.contratos.dto;

public record MiembroDestinatarioResponse(
        Long idMiembro,
        Long idPerfil,
        String nombreArtistico,
        String nombreCompleto,
        String dni,
        String profesion,
        String rolProyecto,
        boolean esEmisor
) {
}
