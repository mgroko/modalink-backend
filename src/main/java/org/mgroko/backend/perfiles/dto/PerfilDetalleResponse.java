package org.mgroko.backend.perfiles.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

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
        CiudadResponse ciudad,
        List<String> habilidades,
        List<CaracteristicaResponse> caracteristicas,
        boolean esPropietario
) {
}
