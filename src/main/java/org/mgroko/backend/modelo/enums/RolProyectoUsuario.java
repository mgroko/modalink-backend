package org.mgroko.backend.modelo.enums;

import java.util.Locale;

import org.mgroko.backend.modelo.RolProyecto;

/**
 * Rol que ocupa el perfil activo dentro de un proyecto.
 * Valores según la semilla de rol_proyecto en ModaLinkBD.sql
 * (codigo 'DIRECTOR' / 'MIEMBRO').
 */
public enum RolProyectoUsuario {
    DIRECTOR,
    MIEMBRO;

    /**
     * Normaliza un rol de proyecto según la semilla de rol_proyecto
     * (ModaLinkBD.sql): primero por codigo ('DIRECTOR'/'MIEMBRO') y, si el rol
     * no tiene codigo, por prefijo del nombre ('Director de proyecto').
     *
     * @param rol rol del miembro; puede ser null
     * @return DIRECTOR o MIEMBRO, o null si el rol es null
     */
    public static RolProyectoUsuario desde(RolProyecto rol) {
        if (rol == null) {
            return null;
        }

        String codigo = rol.getCodigo() != null ? rol.getCodigo().trim().toUpperCase(Locale.ROOT) : "";
        if (DIRECTOR.name().equals(codigo)) {
            return DIRECTOR;
        }
        if (MIEMBRO.name().equals(codigo)) {
            return MIEMBRO;
        }

        // Fallback ante roles sin codigo (ej. datos de prueba)
        String nombre = rol.getNombre() != null ? rol.getNombre().trim().toLowerCase(Locale.ROOT) : "";
        return nombre.startsWith("director") ? DIRECTOR : MIEMBRO;
    }
}
