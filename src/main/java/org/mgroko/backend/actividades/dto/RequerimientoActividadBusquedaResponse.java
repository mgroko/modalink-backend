package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;

/**
 * Fila de UC-73: requerimiento de actividad con los datos mínimos para que el
 * director pueda seleccionarlo al invitar.
 */
public record RequerimientoActividadBusquedaResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        Long idActividad,
        String nombreActividad,
        String estadoActividad,
        Long idProyecto,
        String nombreProyecto,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades
) {

    public RequerimientoActividadBusquedaResponse {
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        habilidades = habilidades == null ? List.of() : List.copyOf(habilidades);
    }
}
