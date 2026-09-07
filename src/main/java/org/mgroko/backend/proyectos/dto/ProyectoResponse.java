package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

public record ProyectoResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        LocalDate fechaFinEstipulada,
        String estado,
        String privacidad,
        Boolean aceptaPostulacionGral,
        UbicacionResponse ubicacion,
        Long idDirector,
        String nombreDirector,
        List<ObjetivoResponse> objetivos
) {}
