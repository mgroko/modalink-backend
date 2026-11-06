package org.mgroko.backend.proyectos.exception;

public class MotivoEliminacionIntegranteObligatorioException extends RuntimeException {

    public MotivoEliminacionIntegranteObligatorioException() {
        super("El motivo de eliminación es obligatorio.");
    }

    public MotivoEliminacionIntegranteObligatorioException(String mensaje) {
        super(mensaje);
    }
}
