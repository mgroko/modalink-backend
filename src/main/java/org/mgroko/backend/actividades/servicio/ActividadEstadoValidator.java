package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.actividades.exception.ActividadNoModificableException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.modelo.enums.EstadoActividad;

/**
 * Regla única de transición de estado de una actividad.
 *
 * <p>La comparten la edición completa y el endpoint específico de cambio de
 * estado para que ambos flujos acepten exactamente la misma matriz.</p>
 */
public final class ActividadEstadoValidator {

    private ActividadEstadoValidator() {
    }

    public static void validar(EstadoActividad estadoActual, EstadoActividad estadoNuevo) {
        if (estadoActual == EstadoActividad.FINALIZADA
                || estadoActual == EstadoActividad.CANCELADA) {
            throw new ActividadNoModificableException(estadoActual.name());
        }

        if (!estadoActual.permiteTransicionA(estadoNuevo)) {
            throw new TransicionEstadoActividadInvalidaException(
                    estadoActual, estadoNuevo);
        }
    }
}
