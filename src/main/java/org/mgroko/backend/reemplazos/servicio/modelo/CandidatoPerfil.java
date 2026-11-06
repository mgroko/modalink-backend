package org.mgroko.backend.reemplazos.servicio.modelo;

import java.util.Map;
import java.util.Set;

/**
 * Datos mínimos de un perfil para evaluar concordancia y ubicación.
 */
public record CandidatoPerfil(
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        String nombre,
        String apellido,
        Long idProfesion,
        String nombreProfesion,
        String fotoUrl,
        Long idCiudad,
        Long idProvincia,
        String nombreCiudad,
        String nombreProvincia,
        Set<Long> idsHabilidades,
        Map<Long, ValorPerfil> caracteristicas) {

    public CandidatoPerfil {
        idsHabilidades = idsHabilidades == null ? Set.of() : Set.copyOf(idsHabilidades);
        caracteristicas = caracteristicas == null ? Map.of() : Map.copyOf(caracteristicas);
    }
}
