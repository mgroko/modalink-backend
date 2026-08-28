package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.modelo.Usuario;

public class AdminUsuarioMapper {

    private AdminUsuarioMapper() {}

    public static AdminUsuarioResponse toResponse(Usuario usuario) {
        return new AdminUsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getEstado().name(),
                usuario.getRolGlobal().getNombre(),
                usuario.getFechaNacimiento(),
                usuario.getDni(),
                usuario.getFechaSolicitudBaja(),
                usuario.getGenero()
        );
    }
}