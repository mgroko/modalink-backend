package org.mgroko.backend.invitaciones.exception;

/**
 * Rango de fechas inválido en la búsqueda de invitaciones (UC-30).
 */
public class RangoFechasInvitacionInvalidoException extends RuntimeException {

    public RangoFechasInvitacionInvalidoException() {
        super("La fecha desde no puede ser posterior a la fecha hasta.");
    }
}
