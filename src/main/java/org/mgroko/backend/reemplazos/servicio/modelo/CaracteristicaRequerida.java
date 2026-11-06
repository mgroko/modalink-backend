package org.mgroko.backend.reemplazos.servicio.modelo;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Característica técnica pedida por un requerimiento, normalizada para el
 * evaluador de concordancia.
 */
public record CaracteristicaRequerida(
        Long idCaracteristica,
        String codigo,
        String nombre,
        String tipoDato,
        BigDecimal valorMin,
        BigDecimal valorMax,
        Set<Long> idsValores) {

    public CaracteristicaRequerida {
        idsValores = idsValores == null ? Set.of() : Set.copyOf(idsValores);
    }
}
