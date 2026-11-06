package org.mgroko.backend.actividades.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Traducción de un cálculo CPM relativo a fechas absolutas usando un inicio
 * de proyecto ({@code t0}).
 *
 * @param inicio    origen de los offsets
 * @param tentativa {@code true} si {@code inicio} no es la fecha de inicio
 *                  confirmada del proyecto; en ese caso las fechas son una
 *                  estimación y deben recalcularse al confirmar el proyecto
 * @param fechas    inicio temprano absoluto por id de actividad
 */
public record ProyeccionCpm(
        LocalDateTime inicio,
        boolean tentativa,
        Map<Long, LocalDateTime> fechas) {

    public ProyeccionCpm {
        fechas = Map.copyOf(fechas);
    }

    /** Fecha proyectada de la actividad o falla si no pertenece a la proyección. */
    public LocalDateTime fechaDe(Long idActividad) {
        LocalDateTime fecha = fechas.get(idActividad);
        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La actividad " + idActividad + " no pertenece a la proyección CPM.");
        }
        return fecha;
    }
}
