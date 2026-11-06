package org.mgroko.backend.reportes.exception;

/**
 * El motivo {@code OTRO} exige una descripción no vacía.
 */
public class DescripcionReporteObligatoriaException extends RuntimeException {

    public DescripcionReporteObligatoriaException() {
        super("La descripción es obligatoria cuando el motivo es OTRO.");
    }
}
