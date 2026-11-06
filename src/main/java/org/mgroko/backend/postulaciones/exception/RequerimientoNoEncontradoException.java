package org.mgroko.backend.postulaciones.exception;

/**
 * El requerimiento indicado no existe o no pertenece al proyecto del path.
 */
public class RequerimientoNoEncontradoException extends RuntimeException {

    public RequerimientoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
