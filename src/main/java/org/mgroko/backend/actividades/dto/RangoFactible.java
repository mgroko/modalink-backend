package org.mgroko.backend.actividades.dto;

/**
 * Ventana factible de una actividad expresada en minutos relativos al inicio
 * del proyecto ({@code t0}). El motor CPM no conoce fechas absolutas: la
 * traducción a un calendario se hace con
 * {@link org.mgroko.backend.actividades.servicio.CronogramaService#proyectar}.
 *
 * <p>El intervalo es cerrado en ambos extremos y representa el rango en el que
 * la actividad puede ejecutarse sin retrasar el proyecto: desde su inicio
 * temprano (ES) hasta su fin tardío (LF).</p>
 *
 * @param inicioMinutos minutos desde {@code t0} hasta el inicio temprano (ES)
 * @param finMinutos    minutos desde {@code t0} hasta el fin tardío (LF)
 */
public record RangoFactible(long inicioMinutos, long finMinutos) {

    /**
     * Duración de la ventana factible en minutos. Siempre es mayor o igual a
     * la duración de la actividad, salvo que existan fechas fijas
     * incompatibles con sus dependencias (holgura negativa).
     */
    public long duracionMinutos() {
        return finMinutos - inicioMinutos;
    }
}
