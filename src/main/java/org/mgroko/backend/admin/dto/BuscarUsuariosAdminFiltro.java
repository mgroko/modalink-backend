package org.mgroko.backend.admin.dto;

import org.mgroko.backend.modelo.enums.EstadoUsuario;

public record BuscarUsuariosAdminFiltro(
        String nombre,
        String apellido,
        String correo,
        EstadoUsuario estado,
        Long idProfesion,
        String nombreProfesion,
        String nombreArtisticoPerfil
) {
}
