package org.mgroko.backend.perfiles.dto;

import java.util.List;

public record BuscarPerfilesFiltro(
        String nombreArtistico,
        String nombre,
        String apellido,
        Long idProfesion,
        String profesion,
        Long idGenero,
        String genero,
        Long idUbicacion,
        String localidad,
        String provincia,
        List<Long> idsHabilidades,
        Long idCaracteristica,
        String valorCaracteristica,
        Long idValorCaracteristica
) {
}
