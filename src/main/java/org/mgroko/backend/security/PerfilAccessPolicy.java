package org.mgroko.backend.security;

import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.security.exception.AccesoDenegadoPorRolException;

/**
 * Reglas de acceso transversales al área de usuario. El rol Administrador
 * gestiona el sistema desde /admin y no posee perfil creativo ni proyectos
 * propios.
 */
public final class PerfilAccessPolicy {

    private PerfilAccessPolicy() {
    }

    /**
     * Corta la operación si el usuario autenticado tiene rol Administrador.
     * Se usa en la capa de servicio como segunda barrera, además de las
     * reglas de URL y de método.
     */
    public static void validarNoAdministrador(Usuario usuario) {
        if (esAdministrador(usuario)) {
            throw new AccesoDenegadoPorRolException(
                    "El rol Administrador no puede operar el área de usuario.");
        }
    }

    public static boolean esAdministrador(Usuario usuario) {
        if (usuario == null || usuario.getRolGlobal() == null) {
            return false;
        }

        RolGlobal rol = usuario.getRolGlobal();
        String codigo = rol.getCodigo() != null ? rol.getCodigo() : rol.getNombre();

        return RolGlobalCodigo.ADMINISTRADOR.equalsIgnoreCase(codigo)
                || "Administrador".equalsIgnoreCase(rol.getNombre());
    }
}
