package org.mgroko.backend.admin.dto;

import org.mgroko.backend.modelo.enums.EstadoPerfil;

public record BuscarPerfilesAdminFiltro(
        String texto,
        Long idProfesion,
        EstadoPerfil estado,
        Long idUsuario,
        Long idGenero,
        Long idCiudad,
        Long idProvincia
) {
}
