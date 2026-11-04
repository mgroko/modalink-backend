package org.mgroko.backend.postulaciones.exception;

/**
 * Regla de negocio de UC-26 incumplida: proyecto que ya no acepta
 * postulaciones, perfil no apto, requerimiento satisfecho, postulación
 * duplicada o falta de disponibilidad.
 */
public class PostulacionNoPermitidaException extends RuntimeException {

    private final String codigo;

    public PostulacionNoPermitidaException(String mensaje) {
        this(mensaje, null);
    }

    public PostulacionNoPermitidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
