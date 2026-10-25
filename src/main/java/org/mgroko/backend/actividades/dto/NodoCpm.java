package org.mgroko.backend.actividades.dto;

/**
 * Resultado del cálculo CPM para una actividad. Todos los instantes son
 * offsets en minutos respecto del inicio del proyecto ({@code t0}); el motor
 * no lee ni escribe fechas absolutas.
 *
 * @param idActividad    identificador de la actividad
 * @param duracionMinutos duración planificada
 * @param inicioTemprano  inicio más temprano posible (ES)
 * @param finTemprano     fin más temprano posible (EF)
 * @param inicioTardio    inicio más tardío sin retrasar el proyecto (LS)
 * @param finTardio       fin más tardío sin retrasar el proyecto (LF)
 * @param holgura         {@code inicioTardio - inicioTemprano}
 * @param critica         holgura nula o negativa; una holgura negativa indica
 *                        fechas fijas incompatibles con sus predecesoras
 * @param inicioFijo      la actividad conserva una fecha ya planificada
 *                        (no está {@code PENDIENTE})
 */
public record NodoCpm(
        Long idActividad,
        int duracionMinutos,
        long inicioTemprano,
        long finTemprano,
        long inicioTardio,
        long finTardio,
        long holgura,
        boolean critica,
        boolean inicioFijo) {

    /** Ventana factible de la actividad: {@code [ES, LF]}. */
    public RangoFactible rangoFactible() {
        return new RangoFactible(inicioTemprano, finTardio);
    }
}
