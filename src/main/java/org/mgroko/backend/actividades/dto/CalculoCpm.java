package org.mgroko.backend.actividades.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado completo de un cálculo CPM en tiempo relativo (minutos desde
 * {@code t0}). Es inmutable y determinista: no depende del reloj ni del estado
 * persistido de las actividades.
 *
 * <p>La viabilidad combina tres condiciones (sección 3.6 del plan v3):</p>
 * <pre>
 * makespan &lt;= T
 * y no hay conflictos con actividades fijas
 * y toda holgura (makespan y entrega) es &gt;= 0
 * </pre>
 *
 * <p>El margen global {@code T - makespan} es solo un indicador: no se usa para
 * calcular {@code LS} ni {@code LF}.</p>
 *
 * @param nodos               resultado por actividad, indexado por id
 * @param makespan            duración de la ruta crítica en minutos
 * @param minutosHastaEntrega {@code T}; {@code null} si el proyecto no tiene
 *                            fecha de entrega
 * @param viable              resultado de la viabilidad; {@code null} si no hay
 *                            entrega y no se puede afirmar nada
 * @param conflictos          conflictos estructurales detectados
 */
public record CalculoCpm(
        Map<Long, NodoCpm> nodos,
        long makespan,
        Long minutosHastaEntrega,
        Boolean viable,
        List<ConflictoCpm> conflictos) {

    public CalculoCpm {
        if (makespan < 0) {
            throw new IllegalArgumentException("El makespan no puede ser negativo.");
        }
        nodos = Collections.unmodifiableMap(new LinkedHashMap<>(nodos));
        conflictos = List.copyOf(conflictos);
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

    /** Ventana de inicio {@code [ES, LS^E]} de la actividad. */
    public RangoFactible ventanaInicio(Long idActividad) {
        return nodoDe(idActividad).ventanaInicio();
    }

    /** Ventana de ejecución {@code [ES, LF^E]} de la actividad. */
    public RangoFactible ventanaEjecucion(Long idActividad) {
        return nodoDe(idActividad).ventanaEjecucion();
    }

    /**
     * Alias histórico de {@link #ventanaInicio(Long)}.
     *
     * @deprecated el nombre “rango factible” no distingue inicio de ejecución.
     */
    @Deprecated
    public RangoFactible rangoFactible(Long idActividad) {
        return ventanaInicio(idActividad);
    }

    /**
     * Margen global contra la entrega, en minutos: {@code T - makespan}.
     * Positivo si sobra tiempo, negativo si el cronograma no entra. Es
     * {@code null} si no hay fecha de entrega.
     */
    public Long margen() {
        return minutosHastaEntrega == null ? null : minutosHastaEntrega - makespan;
    }

    /**
     * Indica que no se ejecutó la pasada de entrega porque falta la fecha de
     * entrega: las ventanas usan la pasada de makespan y son parciales.
     */
    public boolean parcial() {
        return minutosHastaEntrega == null;
    }

    /**
     * Viabilidad contra un plazo externo. Se conserva para los llamadores que
     * calculan los minutos disponibles por su cuenta; la fuente de verdad es
     * {@link #viable()}.
     *
     * @deprecated usar {@link #viable()}, que además valida conflictos y
     *             holguras de entrega.
     */
    @Deprecated
    public boolean esViable(long minutosDisponibles) {
        return makespan <= minutosDisponibles;
    }
}
