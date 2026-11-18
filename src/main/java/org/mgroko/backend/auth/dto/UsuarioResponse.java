package org.mgroko.backend.auth.dto;

import java.time.LocalDate;
import java.util.List;

public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String rolGlobal,
        String genero,
        LocalDate fechaNacimiento,
        String estado,
        Long idPerfilActivo,
        String nombreArtisticoActivo,
        List<String> permisosGlobales
) {
}