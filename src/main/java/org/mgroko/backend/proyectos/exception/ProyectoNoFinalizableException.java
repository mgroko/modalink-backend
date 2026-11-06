package org.mgroko.backend.proyectos.exception;

import java.util.List;

import org.mgroko.backend.proyectos.dto.BloqueoFinalizacionResponse;

/**
 * Excepción de negocio de UC-37. Agrupa un código estable y la lista de
 * bloqueos que impiden finalizar el proyecto.
 */
public class ProyectoNoFinalizableException extends RuntimeException {

    private final String codigo;
    private final List<BloqueoFinalizacionResponse> bloqueos;

    public ProyectoNoFinalizableException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public ProyectoNoFinalizableException(
            String codigo,
            String mensaje,
            List<BloqueoFinalizacionResponse> bloqueos) {
        super(mensaje);
        this.codigo = codigo;
        this.bloqueos = bloqueos == null ? List.of() : List.copyOf(bloqueos);
    }

    public String getCodigo() {
        return codigo;
    }

    public List<BloqueoFinalizacionResponse> getBloqueos() {
        return bloqueos;
    }
}
