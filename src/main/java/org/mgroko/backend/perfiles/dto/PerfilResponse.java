package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PerfilResponse(
        Long idPerfil,
        String nombreArtistico,
        String biografia,
        String estado,
        String profesion,
        LocalDateTime fechaSolicitudBaja,
        List<CaracteristicaResponse> caracteristicas
) {
}