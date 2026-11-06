package org.mgroko.backend.actividades.dto;

/**
 * Ventana cerrada de una actividad expresada en minutos relativos al inicio
 * del proyecto ({@code t0}).
 *
 * <p>El motor CPM no conoce fechas absolutas: la traducción a un calendario se
 * hace con
 * {@link org.mgroko.backend.actividades.servicio.CronogramaService#proyectar}.</p>
 *
 * <p>Este registro no define por sí mismo qué representa cada extremo. La
 * semántica se fija en {@link NodoCpm}:</p>
 * <ul>
 *   <li>{@link NodoCpm#ventanaInicio()}: {@code [ES, LS^E]}, el intervalo en el
 *       que la actividad puede <strong>comenzar</strong>;</li>
 *   <li>{@link NodoCpm#ventanaEjecucion()}: {@code [ES, LF^E]}, el intervalo
 *       en el que la actividad puede <strong>ejecutarse</strong>.</li>
 * </ul>
 *
 * <p>Nunca debe interpretarse como “rango de inicio” un intervalo
 * {@code [ES, LF]}: ese intervalo mezcla el último inicio con el último fin.
 * Cuando la actividad no tiene holgura, la ventana puede quedar invertida
 * ({@code fin < inicio}); en ese caso {@link #factible()} es {@code false} y el
 * nodo expone el motivo en {@link NodoCpm#motivoConflicto()}.</p>
 *
 * @param inicioMinutos minutos desde {@code t0} hasta el extremo inferior
 * @param finMinutos    minutos desde {@code t0} hasta el extremo superior
 */
public record RangoFactible(long inicioMinutos, long finMinutos) {

    /**
     * Duración de la ventana en minutos. Puede ser negativa si el cronograma es
     * infactible (holgura negativa).
     */
    public long duracionMinutos() {
        return finMinutos - inicioMinutos;
    }

    /** Indica si la ventana tiene al menos un instante utilizable. */
    public boolean factible() {
        return finMinutos >= inicioMinutos;
    }
}
