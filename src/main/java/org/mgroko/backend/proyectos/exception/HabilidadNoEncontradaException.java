package org.mgroko.backend.proyectos.exception;

public class HabilidadNoEncontradaException extends RuntimeException {
    public HabilidadNoEncontradaException(Long idHabilidad) {
        super("La habilidad con id " + idHabilidad + " no existe.");
    }
}
