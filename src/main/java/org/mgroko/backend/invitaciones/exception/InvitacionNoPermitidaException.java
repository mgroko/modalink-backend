package org.mgroko.backend.invitaciones.exception;

/**
 * Regla de negocio de UC-31 incumplida: proyecto o actividad en estado
 * inválido, perfil no apto, miembro activo, autoinvitación o duplicado.
 */
public class InvitacionNoPermitidaException extends RuntimeException {

    private final String codigo;

    public InvitacionNoPermitidaException(String mensaje) {
        this(mensaje, null);
    }

    public InvitacionNoPermitidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
