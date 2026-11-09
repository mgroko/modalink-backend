package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;

import org.mgroko.backend.ubicacion.dto.CiudadResponse;

public record ProyectoBusquedaResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        String estado,
        String privacidad,
        Boolean aceptaPostulacionGral,
        Long idDirector,
        String nombreDirector,
        CiudadResponse ciudad
) {}
