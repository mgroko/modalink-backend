package org.mgroko.backend.proyectos.exception;

import java.time.LocalDate;

/**
 * La fecha de entrega todavía no se alcanzó y el director no confirmó
 * explícitamente la finalización anticipada.
 */
public class ConfirmacionPrematuraRequeridaException extends RuntimeException {

    private final LocalDate fechaEntrega;
    private final LocalDate fechaActual;
    private final long diasAnticipacion;

    public ConfirmacionPrematuraRequeridaException(
            LocalDate fechaEntrega,
            LocalDate fechaActual,
            long diasAnticipacion) {
        super("La fecha de entrega estipulada todavía no se alcanzó. "
                + "Confirmá la finalización anticipada para continuar.");
        this.fechaEntrega = fechaEntrega;
        this.fechaActual = fechaActual;
        this.diasAnticipacion = diasAnticipacion;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public LocalDate getFechaActual() {
        return fechaActual;
    }

    public long getDiasAnticipacion() {
        return diasAnticipacion;
    }
}
