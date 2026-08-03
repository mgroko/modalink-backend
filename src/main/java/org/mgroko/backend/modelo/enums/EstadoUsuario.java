package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla usuario.
 * Los nombres de las constantes respetan el string guardado en la base
 * (no la convención ALL_CAPS de Java) para que @Enumerated(EnumType.STRING)
 * escriba y compare exactamente lo que exige el CHECK constraint.
 */
public enum EstadoUsuario {
    Activo,
    Deshabilitado,
    Inactivo,
    Baja
}
