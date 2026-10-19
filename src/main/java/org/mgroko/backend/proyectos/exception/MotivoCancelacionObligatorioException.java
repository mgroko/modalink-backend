package org.mgroko.backend.proyectos.exception;

public class MotivoCancelacionObligatorioException extends RuntimeException {

    public MotivoCancelacionObligatorioException() {
        super("El motivo de cancelación es obligatorio.");
    }
}
