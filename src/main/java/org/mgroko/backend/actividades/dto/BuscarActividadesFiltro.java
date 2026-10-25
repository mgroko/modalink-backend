package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoActividad;

public record BuscarActividadesFiltro(
        String nombre,
        String descripcion,
        String requerimiento,
        Integer duracionMin,
        Integer duracionMax,
        List<EstadoActividad> estado
) {}
