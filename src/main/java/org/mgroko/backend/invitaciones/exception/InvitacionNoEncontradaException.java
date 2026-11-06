package org.mgroko.backend.invitaciones.exception;

/**
 * Invitación inexistente o perteneciente a otro proyecto (UC-32).
 */
public class InvitacionNoEncontradaException extends RuntimeException {

    public InvitacionNoEncontradaException() {
        super("La invitación no existe o no pertenece al proyecto indicado.");
    }
}
