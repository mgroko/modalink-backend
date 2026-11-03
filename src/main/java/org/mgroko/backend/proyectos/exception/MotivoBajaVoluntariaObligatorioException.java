package org.mgroko.backend.proyectos.exception;

public class MotivoBajaVoluntariaObligatorioException extends RuntimeException {

    public MotivoBajaVoluntariaObligatorioException() {
        super("El motivo de la baja es obligatorio.");
    }

    public MotivoBajaVoluntariaObligatorioException(String mensaje) {
        super(mensaje);
    }
}
