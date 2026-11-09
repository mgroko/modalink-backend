package org.mgroko.backend.mensajeria.exception;

/**
 * La conversación no existe o el perfil activo no participa de ella.
 */
public class ConversacionNoEncontradaException extends RuntimeException {

    public ConversacionNoEncontradaException() {
        super("La conversación no existe.");
    }
}
