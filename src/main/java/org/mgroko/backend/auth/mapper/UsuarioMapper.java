package org.mgroko.backend.auth.mapper;

import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.modelo.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return toResponseConPerfilActivo(usuario, null, null);
    }

    public static UsuarioResponse toResponseConPerfilActivo(Usuario usuario, Long idPerfilActivo,
            String nombreArtisticoActivo) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getDni(),
                usuario.getCorreo(),
                usuario.getRolGlobal() != null ? usuario.getRolGlobal().getNombre() : null,
                usuario.getGenero() != null ? usuario.getGenero().getCodigo() : null,
                usuario.getFechaNacimiento(),
                usuario.getEstado() != null ? usuario.getEstado().name() : null,
                idPerfilActivo,
                nombreArtisticoActivo);
    }
}