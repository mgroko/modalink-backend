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
        return desde(rol.getCodigo(), rol.getNombre());
    }

    /**
     * Normaliza un rol a partir de su código y nombre, sin exigir la entidad.
     * Se usa cuando el rol se obtiene como proyección en una consulta batch.
     *
     * @param codigo código del rol; puede ser null
     * @param nombre nombre del rol; puede ser null
     * @return DIRECTOR o MIEMBRO
     */
    public static RolProyectoUsuario desde(String codigo, String nombre) {
        String codigoNormalizado = codigo != null ? codigo.trim().toUpperCase(Locale.ROOT) : "";
        if (DIRECTOR.name().equals(codigoNormalizado)) {
            return DIRECTOR;
        }
        if (MIEMBRO.name().equals(codigoNormalizado)) {
            return MIEMBRO;
        }

        // Fallback ante roles sin codigo (ej. datos de prueba)
        String nombreNormalizado = nombre != null ? nombre.trim().toLowerCase(Locale.ROOT) : "";
        return nombreNormalizado.startsWith("director") ? DIRECTOR : MIEMBRO;
    }
}
