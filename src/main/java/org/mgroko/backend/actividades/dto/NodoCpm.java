package org.mgroko.backend.actividades.dto;

/**
 * Resultado del cálculo CPM para una actividad. Todos los instantes son
 * offsets en minutos respecto del inicio del proyecto ({@code t0}); el motor
 * no lee ni escribe fechas absolutas.
 *
 * <p>El plan v3 distingue dos pasadas y, por lo tanto, dos holguras:</p>
 * <ul>
 *   <li><strong>makespan</strong>: holgura propia del grafo, sin fecha de
 *       entrega. Es la que define la ruta crítica.</li>
 *   <li><strong>entrega</strong>: último inicio y último fin que todavía
 *       permiten entregar en fecha. Es {@code null} cuando el proyecto no
 *       tiene fecha de entrega.</li>
 * </ul>
 *
 * <p>El margen global ({@code T - makespan}) no se suma a ningún {@code LF}:
 * se informa en {@link CalculoCpm#margen()}.</p>
 *
 * @param idActividad           identificador de la actividad
 * @param duracionMinutos       duración planificada
 * @param fijo                  la actividad está anclada en un inicio conocido
 *                              ({@code EN_CURSO}, {@code FINALIZADA} o
 *                              {@code PENDIENTE} con inicio confirmado)
 * @param inicioFijo            {@code S_i} si la actividad es fija; {@code null}
 *                              en caso contrario
 * @param inicioTemprano        inicio más temprano posible (ES)
 * @param finTemprano           fin más temprano posible (EF)
 * @param inicioTardioMakespan  inicio más tardío sin alargar el proyecto (LS^M)
 * @param finTardioMakespan     fin más tardío sin alargar el proyecto (LF^M)
 * @param holguraMakespan       {@code LS^M - ES}
 * @param critica               {@code holguraMakespan == 0}; una actividad fija
 *                              siempre tiene holgura cero, aunque no determine
 *                              el makespan
 * @param inicioTardioEntrega   inicio más tardío que aún permite entregar
 *                              (LS^E); {@code null} sin fecha de entrega
 * @param finTardioEntrega      fin más tardío que aún permite entregar (LF^E);
 *                              {@code null} sin fecha de entrega
 * @param holguraEntrega        {@code LS^E - ES}; {@code null} sin entrega
 * @param conflicto             holgura negativa o choque con una actividad fija
 * @param motivoConflicto       detalle del conflicto; {@code null} si no hay
 * @param enRutaCritica         pertenece a la cadena que determina el makespan
 */
public record NodoCpm(
        Long idActividad,
        int duracionMinutos,
        boolean fijo,
        Long inicioFijo,
        long inicioTemprano,
        long finTemprano,
        long inicioTardioMakespan,
        long finTardioMakespan,
        long holguraMakespan,
        boolean critica,
        Long inicioTardioEntrega,
        Long finTardioEntrega,
        Long holguraEntrega,
        boolean conflicto,
        String motivoConflicto,
        boolean enRutaCritica) {

    /**
     * Ventana de inicio {@code [ES, LS^E]} (o {@code [S, S]} si es fija).
     * Sin fecha de entrega usa la pasada de makespan y queda marcada como
     * parcial en {@link CalculoCpm#parcial()}.
     */
    public RangoFactible ventanaInicio() {
        long fin = inicioTardioEntrega != null ? inicioTardioEntrega : inicioTardioMakespan;
        return new RangoFactible(inicioTemprano, fin);
    }

    /**
     * Ventana de ejecución {@code [ES, LF^E]} (o {@code [S, EF]} si es fija).
     * Sin fecha de entrega usa la pasada de makespan.
     */
    public RangoFactible ventanaEjecucion() {
        long fin = finTardioEntrega != null ? finTardioEntrega : finTardioMakespan;
        return new RangoFactible(inicioTemprano, fin);
    }

    /**
     * Alias histórico de {@link #ventanaInicio()}.
     *
     * @deprecated el nombre “rango factible” no distingue inicio de ejecución;
     *             usar {@link #ventanaInicio()} o {@link #ventanaEjecucion()}.
     */
    @Deprecated
    public RangoFactible rangoFactible() {
        return ventanaInicio();
    }
}
