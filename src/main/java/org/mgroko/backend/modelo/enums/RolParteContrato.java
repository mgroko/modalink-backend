package org.mgroko.backend.modelo.enums;

/**
 * Rol de una parte dentro de un contrato.
 *
 * <p>El director emisor siempre participa: si no fue seleccionado como
 * destinatario queda como {@code DIRECTOR}; si fue seleccionado, el trigger de
 * emision lo convierte en {@code DIRECTOR_DESTINATARIO}.</p>
 */
public enum RolParteContrato {
    DIRECTOR,
    DESTINATARIO,
    DIRECTOR_DESTINATARIO;

    public boolean esDestinatario() {
        return this == DESTINATARIO || this == DIRECTOR_DESTINATARIO;
    }
}
