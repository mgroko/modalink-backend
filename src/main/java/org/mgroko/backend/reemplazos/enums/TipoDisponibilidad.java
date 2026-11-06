package org.mgroko.backend.reemplazos.enums;

/**
 * Resultado de disponibilidad de un candidato para una actividad.
 */
public enum TipoDisponibilidad {
    EXACTA(0),
    CON_REPROGRAMACION(1),
    NO_APLICA(0),
    NO_DISPONIBLE(2);

    private final int orden;

    TipoDisponibilidad(int orden) {
        this.orden = orden;
    }

    public int getOrden() {
        return orden;
    }

    public boolean disponible() {
        return this == EXACTA
                || this == CON_REPROGRAMACION
                || this == NO_APLICA;
    }
}
