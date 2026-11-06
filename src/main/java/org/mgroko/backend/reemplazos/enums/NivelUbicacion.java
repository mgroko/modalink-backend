package org.mgroko.backend.reemplazos.enums;

/**
 * Cercanía de la ubicación del candidato respecto de la ubicación efectiva
 * de la vacante.
 */
public enum NivelUbicacion {
    MISMA_CIUDAD(0),
    MISMA_PROVINCIA(1),
    OTRA(2);

    private final int orden;

    NivelUbicacion(int orden) {
        this.orden = orden;
    }

    public int getOrden() {
        return orden;
    }
}
