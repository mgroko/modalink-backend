package org.mgroko.backend.perfiles.exception;

/**
 * La reseña no existe o no pertenece al perfil consultado.
 */
public class ResenaNoEncontradaException extends RuntimeException {

    public ResenaNoEncontradaException() {
        super("La reseña no fue encontrada.");
    }
}
