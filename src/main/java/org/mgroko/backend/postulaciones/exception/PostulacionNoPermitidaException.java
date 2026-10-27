package org.mgroko.backend.postulaciones.exception;

/**
 * Regla de negocio de UC-26 incumplida: proyecto que ya no acepta
 * postulaciones, perfil no apto, requerimiento satisfecho, postulación
 * duplicada o falta de disponibilidad.
 */
public class PostulacionNoPermitidaException extends RuntimeException {

    public PostulacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
