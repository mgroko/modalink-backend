package org.mgroko.backend.actividades.mapper;

import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class ActividadMapper {

    private ActividadMapper() {}

    public static ActividadBusquedaResponse toBusquedaResponse(
            Actividad actividad,
            Proyecto proyecto,
            List<RequerimientoActividadResumenResponse> requerimientos,
            List<ActividadVinculadaResponse> predecesoras) {

        return new ActividadBusquedaResponse(
                actividad.getIdActividad(),
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                actividad.getDuracionMinutos(),
                actividad.getFechaHoraInicio(),
                actividad.getFechaHoraFin(),
                actividad.getHorarioPreferenciaInicio(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                requerimientos != null ? requerimientos : List.of(),
                predecesoras != null ? predecesoras : List.of()
        );
    }

    public static RequerimientoActividadResumenResponse toRequerimientoResumen(
            RequerimientoActividad requerimiento) {
        return new RequerimientoActividadResumenResponse(
                requerimiento.getIdRequerimiento(),
                requerimiento.getCantidad(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion() != null ? requerimiento.getProfesion().getIdProfesion() : null,
                requerimiento.getProfesion() != null ? requerimiento.getProfesion().getNombre() : null
        );
    }

    public static ActividadResponse toResponse(
            Actividad actividad,
            List<RequerimientoActividadResponse> requerimientos) {
        return toResponse(actividad, requerimientos, List.of(), EstadoCronograma.NORMAL);
    }

    public static ActividadResponse toResponse(
            Actividad actividad,
            List<RequerimientoActividadResponse> requerimientos,
            List<ProfesionalAsignadoResponse> profesionalesAsignados) {
        return toResponse(
                actividad, requerimientos, profesionalesAsignados, EstadoCronograma.NORMAL);
    }

    public static ActividadResponse toResponse(
            Actividad actividad,
            List<RequerimientoActividadResponse> requerimientos,
            List<ProfesionalAsignadoResponse> profesionalesAsignados,
            EstadoCronograma estadoCronograma) {

        Proyecto proyecto = actividad.getPlanificacion().getProyecto();

        List<ActividadVinculadaResponse> predecesoras = actividad.getPredecesoras().stream()
                .sorted(Comparator.comparing(Actividad::getNombre))
                .map(predecesora -> new ActividadVinculadaResponse(
                        predecesora.getIdActividad(), predecesora.getNombre()))
                .toList();

        List<ProfesionalAsignadoResponse> profesionales = profesionalesAsignados.stream()
                .sorted(Comparator
                        .comparing(
                                ProfesionalAsignadoResponse::nombreArtistico,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(
                                ProfesionalAsignadoResponse::idPerfil,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();

        return new ActividadResponse(
                actividad.getIdActividad(),
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                actividad.getDuracionMinutos(),
                actividad.getFechaHoraInicio(),
                actividad.getFechaHoraFin(),
                actividad.getHorarioPreferenciaInicio(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                (estadoCronograma != null ? estadoCronograma : EstadoCronograma.NORMAL).name(),
                UbicacionMapper.toResponse(actividad.getUbicacion()),
                predecesoras,
                requerimientos != null ? requerimientos : List.of(),
                profesionales
        );
    }

    public static ProfesionalAsignadoResponse toProfesionalAsignado(
            AsignacionActividad asignacion) {

        MiembroProyecto miembro = asignacion.getMiembro();
        Perfil perfil = miembro.getPerfil();
        Usuario usuario = perfil.getUsuario();
        Profesion profesion = perfil.getProfesion();

        return new ProfesionalAsignadoResponse(
                asignacion.getIdAsignacionAct(),
                miembro.getIdMiembro(),
                perfil.getIdPerfil(),
                usuario != null ? usuario.getIdUsuario() : null,
                perfil.getNombreArtistico(),
                profesion != null ? profesion.getIdProfesion() : null,
                profesion != null ? profesion.getNombre() : null
        );
    }
}
