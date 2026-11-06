package org.mgroko.backend.proyectos.exception;

import java.util.List;

import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;

/**
 * Excepción de negocio de UC-60. Agrupa un código estable y la lista de
 * bloqueos que impiden confirmar el proyecto.
 */
public class ProyectoNoConfirmableException extends RuntimeException {

    private final String codigo;
    private final List<BloqueoConfirmacionResponse> bloqueos;

    public ProyectoNoConfirmableException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ProyectoNoConfirmableException(
            String codigo,
            String mensaje,
            List<BloqueoConfirmacionResponse> bloqueos) {
        super(mensaje);
        this.codigo = codigo;
        this.bloqueos = bloqueos == null ? List.of() : List.copyOf(bloqueos);
    }

    public String getCodigo() {
        return codigo;
    }

    public List<BloqueoConfirmacionResponse> getBloqueos() {
        return bloqueos;
    }
}
