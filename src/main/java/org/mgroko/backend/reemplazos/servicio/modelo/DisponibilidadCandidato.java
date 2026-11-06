package org.mgroko.backend.reemplazos.servicio.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;

/**
 * Disponibilidad calculada para un candidato frente a una vacante.
 */
public record DisponibilidadCandidato(
        TipoDisponibilidad tipo,
        LocalDateTime inicio,
        LocalDateTime fin) {

    public static DisponibilidadCandidato noAplica() {
        return new DisponibilidadCandidato(TipoDisponibilidad.NO_APLICA, null, null);
    }

    public static DisponibilidadCandidato noDisponible() {
        return new DisponibilidadCandidato(TipoDisponibilidad.NO_DISPONIBLE, null, null);
    }

    public boolean disponible() {
        return tipo != null && tipo.disponible();
    }
}
