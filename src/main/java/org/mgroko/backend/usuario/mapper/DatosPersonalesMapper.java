package org.mgroko.backend.usuario.mapper;

import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;

public final class DatosPersonalesMapper {

    private DatosPersonalesMapper() {
    }

    public static DatosPersonalesResponse toResponse(Usuario usuario) {
        String ubicacion = null;
        if (usuario.getUbicacion() != null) {
            ubicacion = usuario.getUbicacion().getLocalidad() + ", "
                    + usuario.getUbicacion().getProvincia();
        }

        return new DatosPersonalesResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFechaNacimiento(),
                usuario.getGenero() != null ? usuario.getGenero().getCodigo() : null,
                ubicacion
        );
    }
}
