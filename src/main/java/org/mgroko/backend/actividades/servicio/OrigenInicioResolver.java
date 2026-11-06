package org.mgroko.backend.actividades.servicio;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;

/**
 * Regla única para calcular el origen efectivo de una actividad.
 *
 * <p>El origen no se persiste: se deriva del estado de la actividad, de su
 * inicio persistido, de las asignaciones activas y del estado del proyecto.
 * Solo {@link EstadoActividad#EN_CURSO} y
 * {@link EstadoActividad#FINALIZADA} son inmóviles; toda
 * {@link EstadoActividad#PENDIENTE} es reprogramable.</p>
 *
 * <p>Para el cálculo de asignaciones activas se cuenta un miembro activo con
 * requerimiento activo y una asignación directa sin requerimiento. Un
 * requerimiento archivado no cuenta.</p>
 */
public final class OrigenInicioResolver {

    private OrigenInicioResolver() {
    }

    /**
     * Calcula el origen efectivo de una actividad.
     *
     * @param actividad               actividad evaluada
     * @param proyecto                proyecto al que pertenece la actividad
     * @param tieneAsignacionesActivas {@code true} si la actividad tiene al
     *                                menos una asignación de un miembro activo
     *                                sobre un requerimiento activo (o una
     *                                asignación directa sin requerimiento)
     * @return origen efectivo, o {@code null} si la actividad está cancelada
     */
    public static OrigenInicio resolver(
            Actividad actividad,
            Proyecto proyecto,
            boolean tieneAsignacionesActivas) {

        return resolver(
                actividad,
                proyecto != null ? proyecto.getEstado() : null,
                tieneAsignacionesActivas);
    }

    /**
     * Variante que recibe directamente el estado del proyecto. Útil para
     * mapeos y tests donde no se necesita cargar la entidad completa.
     */
    public static OrigenInicio resolver(
            Actividad actividad,
            EstadoProyecto estadoProyecto,
            boolean tieneAsignacionesActivas) {

        if (actividad == null || actividad.getEstado() == null) {
            return null;
        }

        if (actividad.getEstado() == EstadoActividad.CANCELADA) {
            return null;
        }

        if (actividad.getEstado() == EstadoActividad.EN_CURSO
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            return OrigenInicio.REAL;
        }

        // PENDIENTE sin horario persistido: todavía no hay inicio que confirmar.
        if (actividad.getFechaHoraInicio() == null) {
            return OrigenInicio.PROYECTADA;
        }

        // Sin proyecto confirmado no hay plan comprometido.
        if (estadoProyecto != EstadoProyecto.CONFIRMADO) {
            return OrigenInicio.PROYECTADA;
        }

        return tieneAsignacionesActivas
                ? OrigenInicio.CONFIRMADA
                : OrigenInicio.PROYECTADA;
    }

    /**
     * Indica si la actividad no puede reprogramarse.
     *
     * <p>Solo las actividades en ejecución o finalizadas conservan su inicio
     * real. Las pendientes son siempre reprogramables, sin importar su origen
     * calculado.</p>
     */
    public static boolean esInmovible(Actividad actividad) {
        return actividad != null
                && (actividad.getEstado() == EstadoActividad.EN_CURSO
                        || actividad.getEstado() == EstadoActividad.FINALIZADA);
    }
}
