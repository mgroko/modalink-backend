package org.mgroko.backend.auditoria.exception;

/**
 * Error de validación de los filtros de UC-67.
 */
public class AuditoriaFiltroInvalidoException extends RuntimeException {

    private final String codigo;

    public AuditoriaFiltroInvalidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
