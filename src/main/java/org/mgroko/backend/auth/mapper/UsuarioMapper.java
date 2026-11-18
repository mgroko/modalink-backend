package org.mgroko.backend.auth.mapper;

import java.util.List;

import org.mgroko.backend.auth.dto.UsuarioResponse;
import org.mgroko.backend.modelo.PermisoGlobal;
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
                nombreArtisticoActivo,
                permisosGlobales(usuario));
    }

    private static List<String> permisosGlobales(Usuario usuario) {
        if (usuario.getRolGlobal() == null || usuario.getRolGlobal().getPermisos() == null) {
            return List.of();
        }
        return usuario.getRolGlobal().getPermisos().stream()
                .map(PermisoGlobal::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .sorted()
                .toList();
    }
}