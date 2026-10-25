package org.mgroko.backend.actividades.dto;

import java.util.Map;

/**
 * Resultado completo de un cálculo CPM en tiempo relativo (minutos desde
 * {@code t0}). Es inmutable y determinista: no depende del reloj ni del estado
 * persistido de las actividades.
 *
 * @param nodos    resultado por actividad, indexado por id
 * @param makespan  duración de la ruta crítica en minutos
 */
public record CalculoCpm(Map<Long, NodoCpm> nodos, long makespan) {

    public CalculoCpm {
        nodos = Map.copyOf(nodos);
        if (makespan < 0) {
            throw new IllegalArgumentException("El makespan no puede ser negativo.");
        }
    }

    /** Devuelve el nodo de la actividad o falla si no pertenece al cálculo. */
    public NodoCpm nodoDe(Long idActividad) {
        NodoCpm nodo = nodos.get(idActividad);
        if (nodo == null) {
            throw new IllegalArgumentException(
                    "La actividad " + idActividad + " no pertenece al cálculo CPM.");
        }
        return nodo;
    }

    /** Ventana factible {@code [ES, LF]} de la actividad. */
    public RangoFactible rangoFactible(Long idActividad) {
        return nodoDe(idActividad).rangoFactible();
    }

    /**
     * Indica si el proyecto entra en el tiempo disponible.
     *
     * @param minutosDisponibles minutos entre {@code t0} y la fecha de entrega
     */
    public boolean esViable(long minutosDisponibles) {
        return makespan <= minutosDisponibles;
    }

    /**
     * Margen contra la entrega en minutos: positivo si sobra tiempo, negativo
     * si el cronograma no entra.
     */
    public long margen(long minutosDisponibles) {
        return minutosDisponibles - makespan;
    }
}
