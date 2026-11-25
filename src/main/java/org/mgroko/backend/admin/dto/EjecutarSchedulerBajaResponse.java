package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

/**
 * Resultado visible para el administrador al ejecutar manualmente el scheduler
 * de bajas definitivas.
 */
public record EjecutarSchedulerBajaResponse(
        int registrosAfectados,
        LocalDateTime ejecutadoEn,
        String mensaje,
        DetalleBajaScheduler detalle
) {

    public record DetalleBajaScheduler(
            int cuentasExpiradas,
            int perfilesExpirados,
            int proyectosCancelados,
            int membresiasBajaVoluntaria,
            int publicacionesEliminadas,
            int conexionesEliminadas
    ) {
    }
}
