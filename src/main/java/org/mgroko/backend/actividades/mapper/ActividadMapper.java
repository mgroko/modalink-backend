package org.mgroko.backend.actividades.mapper;

import java.util.List;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;

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
}
