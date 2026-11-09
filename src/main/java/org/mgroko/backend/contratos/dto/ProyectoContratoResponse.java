package org.mgroko.backend.contratos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProyectoContratoResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        String estado,
        LocalDateTime fechaInicio,
        LocalDate fechaEntrega
) {
}
