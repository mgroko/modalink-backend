package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;

public record RequerimientoActividadResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades
) {
}
