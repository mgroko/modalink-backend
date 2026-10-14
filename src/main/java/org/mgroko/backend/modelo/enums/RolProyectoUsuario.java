package org.mgroko.backend.modelo.enums;

/**
 * Rol que ocupa el perfil activo dentro de un proyecto.
 * Valores según la semilla de rol_proyecto en ModaLinkBD.sql
 * (codigo 'DIRECTOR' / 'MIEMBRO').
 */
public enum RolProyectoUsuario {
    DIRECTOR,
    MIEMBRO;
}
