package org.mgroko.backend.proyectos.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.ActividadResumenResponse;
import org.mgroko.backend.proyectos.dto.ContextoConsultaProyecto;
import org.mgroko.backend.proyectos.dto.ImagenMoodboardResponse;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ObjetivoResponse;
import org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class ProyectoMapper {

    private ProyectoMapper() {}

    public static ProyectoBusquedaResponse toBusquedaResponse(Proyecto proyecto, Perfil director) {
        return new ProyectoBusquedaResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toLocalDate() : null,
                proyecto.getEstado() != null ? proyecto.getEstado().getNombre() : null,
                proyecto.getPrivacidad() != null ? proyecto.getPrivacidad().name() : null,
                proyecto.getAceptaPostulacionGral(),
                director != null ? director.getIdPerfil() : null,
                director != null ? director.getNombreArtistico() : null
        );
    }

    public static ProyectoPropioResponse toPropioResponse(
            Proyecto proyecto,
            RolProyectoUsuario rolUsuario,
            ActividadResumenResponse proximaActividad) {

        return new ProyectoPropioResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toLocalDate() : null,
                proyecto.getEstado() != null ? proyecto.getEstado().getNombre() : null,
                proyecto.getPrivacidad() != null ? proyecto.getPrivacidad().name() : null,
                rolUsuario,
                proximaActividad
        );
    }

    public static ActividadResumenResponse toActividadResumen(Actividad actividad) {
        return new ActividadResumenResponse(
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getFechaHoraInicio(),
                actividad.getDuracionMinutos()
        );
    }

    public static MoodboardResponse toMoodboardResponse(Moodboard moodboard) {
        List<ImagenMoodboardResponse> imagenes = moodboard.getImagenes() != null
                ? moodboard.getImagenes().stream()
                        .sorted(Comparator.comparing(Imagen::getIdImagen))
                        .map(imagen -> new ImagenMoodboardResponse(imagen.getIdImagen(), imagen.getUrl()))
                        .toList()
                : Collections.emptyList();

        return new MoodboardResponse(
                moodboard.getIdMoodboard(),
                moodboard.getDescripcion(),
                moodboard.getFechaCreacion(),
                imagenes
        );
    }

    public static ProyectoResponse toResponse(
            Proyecto proyecto,
            Perfil director,
            LocalDate fechaEntregaEstipulada,
            List<Objetivo> objetivos,
            List<RequerimientoGralResponse> requerimientosGral,
            MoodboardResponse moodboard,
            ContextoConsultaProyecto contexto) {

        return toResponse(
                proyecto,
                director,
                fechaEntregaEstipulada,
                objetivos,
                requerimientosGral,
                moodboard,
                contexto,
                null);
    }

    public static ProyectoResponse toResponse(
            Proyecto proyecto,
            Perfil director,
            LocalDate fechaEntregaEstipulada,
            List<Objetivo> objetivos,
            List<RequerimientoGralResponse> requerimientosGral,
            MoodboardResponse moodboard,
            ContextoConsultaProyecto contexto,
            Long postulacionesPendientes) {

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
                proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toLocalDate() : null,
                fechaEntregaEstipulada,
                proyecto.getEstado().getNombre(),
                proyecto.getPrivacidad().name(),
                proyecto.getAceptaPostulacionGral(),
                ubicacionResponse,
                director != null ? director.getIdPerfil() : null,
                director != null ? director.getNombreArtistico() : null,
                objetivosResponse,
                requerimientosGral != null ? requerimientosGral : Collections.emptyList(),
                moodboard,
                contexto.rolUsuario(),
                contexto.esMiembro(),
                contexto.permisos(),
                contexto.elegibleParaPostularse(),
                proyecto.getMotivoCancelacion(),
                postulacionesPendientes
        );
    }
}
