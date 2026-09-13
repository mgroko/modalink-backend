package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PerfilDetalleResponse(
        Long idPerfil,
        String nombreArtistico,
        String biografia,
        String estado,
        LocalDateTime fechaSolicitudBaja,
        Long idProfesion,
        String profesion,
        Long idImagen,
        String fotoUrl,
        Long idUsuario,
        String nombreUsuario,
        String apellidoUsuario,
        String genero,
        String localidad,
        String provincia,
        List<String> habilidades,
        List<CaracteristicaResponse> caracteristicas,
        boolean esPropietario
) {
}
