package org.mgroko.backend.conexiones.exception;

/**
 * Solicitud de conexión inexistente o no visible para el perfil activo.
 */
public class SolicitudConexionNoEncontradaException extends RuntimeException {

    public SolicitudConexionNoEncontradaException() {
        super("La solicitud de conexión no existe.");
    }

    public SolicitudConexionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
