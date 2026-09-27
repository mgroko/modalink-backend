package org.mgroko.backend.modelo.enums;

/**
 * Valores exactos según el CHECK de la tabla proyecto en ModaLinkBD.sql:
 * CHECK (privacidad IN ('PUBLICO', 'PRIVADO', 'OCULTO'))
 */
public enum Privacidad {
    PUBLICO,
    PRIVADO,
    OCULTO;

    public static final Privacidad Publico = PUBLICO;
    public static final Privacidad Privado = PRIVADO;
    public static final Privacidad Oculto = OCULTO;
}
