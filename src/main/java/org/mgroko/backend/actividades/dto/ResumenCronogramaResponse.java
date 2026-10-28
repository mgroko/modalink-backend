package org.mgroko.backend.actividades.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen del cronograma para las tarjetas superiores de UC-33.
 *
 * @param fechaFinProyectada fin exclusivo ({@code t0 + makespan}); la UI lo
 *                           muestra como el último día incluido
 * @param margenMinutos      {@code T - makespan}; puede ser negativo
 * @param tentativo          el proyecto todavía no tiene fecha de inicio
 *                           confirmada
 * @param parcial            no hay fecha de entrega completa
 * @param viable             {@code null} si no se puede afirmar sin entrega
 * @param rutaCritica        ids con {@code enRutaCritica}, ordenados por ES
 */
public record ResumenCronogramaResponse(
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinProyectada,
        LocalDate fechaEntrega,
        long makespanMinutos,
        Long margenMinutos,
        boolean tentativo,
        boolean parcial,
        String estadoCronograma,
        Boolean viable,
        int totalActividades,
        int actividadesCriticas,
        List<Long> rutaCritica) {

    public ResumenCronogramaResponse {
        rutaCritica = rutaCritica == null ? List.of() : List.copyOf(rutaCritica);
    }
}
