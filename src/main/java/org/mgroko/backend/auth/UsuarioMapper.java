package org.mgroko.backend.auth;

import org.mgroko.backend.dto.UsuarioResponse;
import org.mgroko.backend.modelo.Usuario;

final class UsuarioMapper {

    private UsuarioMapper() {
    }

    static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getDni(),
                usuario.getCorreo(),
                usuario.getRolGlobal() != null ? usuario.getRolGlobal().getNombre() : null
        );
    }
}