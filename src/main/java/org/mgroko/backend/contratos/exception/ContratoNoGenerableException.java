package org.mgroko.backend.contratos.exception;

import java.util.List;

import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;

/**
 * Excepcion de negocio de UC-39. Agrupa un codigo estable y, cuando
 * corresponde, los conflictos por miembro (por ejemplo, contrato activo
 * duplicado del mismo tipo).
 */
public class ContratoNoGenerableException extends RuntimeException {

    private final String codigo;
    private final List<ConflictoContratoResponse> conflictos;

    public ContratoNoGenerableException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ContratoNoGenerableException(
            String codigo,
            String mensaje,
            List<ConflictoContratoResponse> conflictos) {
        super(mensaje);
        this.codigo = codigo;
        this.conflictos = conflictos == null ? List.of() : List.copyOf(conflictos);
    }

    public String getCodigo() {
        return codigo;
    }

    public List<ConflictoContratoResponse> getConflictos() {
        return conflictos;
    }
}
