package org.mgroko.backend.usuario.mapper;

import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class DatosPersonalesMapper {

    private DatosPersonalesMapper() {
    }

    public static DatosPersonalesResponse toResponse(Usuario usuario) {
        return new DatosPersonalesResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFechaNacimiento(),
                usuario.getGenero() != null ? usuario.getGenero().getCodigo() : null,
                UbicacionMapper.toResponse(usuario.getUbicacion())
        );
    }
}
