package org.mgroko.backend.actividades.exception;

public class MotivoEliminacionObligatorioException extends RuntimeException {

    public MotivoEliminacionObligatorioException() {
        super("El motivo de eliminación es obligatorio.");
    }

    public MotivoEliminacionObligatorioException(String mensaje) {
        super(mensaje);
    }
}
