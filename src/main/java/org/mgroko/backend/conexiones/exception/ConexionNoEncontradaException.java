package org.mgroko.backend.conexiones.exception;

/**
 * La conexión no existe o el perfil activo no participa de ella.
 */
public class ConexionNoEncontradaException extends RuntimeException {

    public ConexionNoEncontradaException() {
        super("La conexión no existe.");
    }
}
