package org.mgroko.backend.proyectos.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.proyectos.dto.ObjetivoResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class ProyectoMapper {

    private ProyectoMapper() {}

    public static ProyectoResponse toResponse(
            Proyecto proyecto,
            Perfil director,
            LocalDate fechaFinEstipulada,
            List<Objetivo> objetivos) {

        UbicacionResponse ubicacionResponse = proyecto.getUbicacion() != null
                ? UbicacionMapper.toResponse(proyecto.getUbicacion())
                : null;

        List<ObjetivoResponse> objetivosResponse = objetivos != null
                ? objetivos.stream()
                        .map(obj -> new ObjetivoResponse(obj.getIdObjetivo(), obj.getNombre(), obj.getDescripcion()))
                        .toList()
                : Collections.emptyList();

        return new ProyectoResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getFechaInicio(),
                fechaFinEstipulada,
                proyecto.getEstado().name(),
                proyecto.getPrivacidad().name(),
                proyecto.getAceptaPostulacionGral(),
                ubicacionResponse,
                director != null ? director.getIdPerfil() : null,
                director != null ? director.getNombreArtistico() : null,
                objetivosResponse
        );
    }
}
