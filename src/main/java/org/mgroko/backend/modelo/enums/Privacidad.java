package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla proyecto en ModaLinkBD.sql:
 * CHECK (privacidad IN ('PUBLICO', 'PRIVADO'))
 */
public enum Privacidad {
    PUBLICO,
    PRIVADO;

    public static final Privacidad Publico = PUBLICO;
    public static final Privacidad Privado = PRIVADO;
}
