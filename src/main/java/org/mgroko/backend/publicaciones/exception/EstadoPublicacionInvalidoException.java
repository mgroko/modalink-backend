package org.mgroko.backend.publicaciones.exception;

/**
 * La publicación ya se encuentra en el estado solicitado y no admite la
 * transición de moderación.
 */
public class EstadoPublicacionInvalidoException extends RuntimeException {

    public EstadoPublicacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
