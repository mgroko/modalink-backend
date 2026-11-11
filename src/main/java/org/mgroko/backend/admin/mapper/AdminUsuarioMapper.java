package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.dto.GeneroResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Usuario;

public class AdminUsuarioMapper {

    private AdminUsuarioMapper() {}

    public static AdminUsuarioResponse toResponse(Usuario usuario) {
        return new AdminUsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getEstado().getNombre(),
                usuario.getRolGlobal().getNombre(),
                usuario.getFechaNacimiento(),
                usuario.getDni(),
                usuario.getFechaSolicitudBaja(),
                usuario.getMotivoDeshabilitacion(),
                usuario.getFechaHastaDeshabilitacion(),
                toGeneroResponse(usuario.getGenero())
        );
    }

    /**
     * Accede a los campos del género dentro de la transacción para inicializar
     * el proxy lazy antes de salir del servicio y lo devuelve como DTO plano.
     */
    private static GeneroResponse toGeneroResponse(Genero genero) {
        if (genero == null) {
            return null;
        }
        return new GeneroResponse(
                genero.getIdGenero(),
                genero.getNombre(),
                genero.getCodigo());
    }
}
