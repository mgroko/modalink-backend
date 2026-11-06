package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.actividades.exception.ActividadNoModificableException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.modelo.Actividad;
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

    /**
     * Valida la transición y, si la actividad pasa a ejecución o finalización,
     * exige que todas sus predecesoras no canceladas estén FINALIZADA.
     */
    public static void validar(Actividad actividad, EstadoActividad estadoNuevo) {
        validar(actividad.getEstado(), estadoNuevo);
        validarPredecesorasFinalizadas(actividad, estadoNuevo);
    }

    /**
     * Valida únicamente la regla de predecesoras para pasar a ejecución o
     * finalización. Se usa después de reemplazar las predecesoras en la
     * edición completa.
     */
    public static void validarPredecesorasFinalizadas(
            Actividad actividad,
            EstadoActividad estadoNuevo) {

        if (estadoNuevo != EstadoActividad.EN_CURSO
                && estadoNuevo != EstadoActividad.FINALIZADA) {
            return;
        }

        for (Actividad predecesora : actividad.getPredecesoras()) {
            if (predecesora == null
                    || predecesora.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            if (predecesora.getEstado() != EstadoActividad.FINALIZADA) {
                throw new TransicionEstadoActividadInvalidaException(
                        "No se puede cambiar la actividad " + actividad.getIdActividad()
                                + " a " + estadoNuevo
                                + " porque su predecesora "
                                + predecesora.getIdActividad() + " ('"
                                + predecesora.getNombre() + "') está en estado "
                                + predecesora.getEstado()
                                + ". Primero debe finalizar la predecesora.");
            }
        }
    }
}
