package org.mgroko.backend.modelo.enums;

/**
 * Estados de un reporte. Los códigos coinciden exactamente con el CHECK de
 * {@code reporte.estado}.
 */
public enum EstadoReporte {

    PENDIENTE("Pendiente"),
    RESUELTO("Resuelto"),
    DESCARTADO("Descartado");

    private final String descripcion;

    EstadoReporte(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
