package org.mgroko.backend.actividades.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.ActividadPlanificacionResponse;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
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
            RequerimientoActividad requerimiento,
            CupoRequerimiento cupo,
            boolean puedePostularse) {
        return toRequerimientoResumen(requerimiento, cupo, puedePostularse, Map.of());
    }

    public static RequerimientoActividadResumenResponse toRequerimientoResumen(
            RequerimientoActividad requerimiento,
            CupoRequerimiento cupo,
            boolean puedePostularse,
            Map<Long, List<Long>> valoresPorRequerimientoCaract) {

        List<CaracteristicaRequerimientoResponse> caracteristicas =
                requerimiento.getCaracteristicas().stream()
                        .map(caracteristica -> toCaracteristica(
                                caracteristica, valoresPorRequerimientoCaract))
                        .toList();

        List<Long> habilidades = requerimiento.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        return new RequerimientoActividadResumenResponse(
                requerimiento.getIdRequerimiento(),
                requerimiento.getCantidad(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion() != null ? requerimiento.getProfesion().getIdProfesion() : null,
                requerimiento.getProfesion() != null ? requerimiento.getProfesion().getNombre() : null,
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                puedePostularse,
                caracteristicas,
                habilidades
        );
    }

    private static CaracteristicaRequerimientoResponse toCaracteristica(
            RequerimientoActCaract caracteristica,
            Map<Long, List<Long>> valoresPorRequerimientoCaract) {

        return new CaracteristicaRequerimientoResponse(
                caracteristica.getCaracteristica().getIdCaracteristica(),
                caracteristica.getCaracteristica().getCodigo(),
                caracteristica.getCaracteristica().getTipoDato(),
                caracteristica.getValorMin(),
                caracteristica.getValorMax(),
                valoresPorRequerimientoCaract.getOrDefault(
                        caracteristica.getIdReqActCaract(), List.of()));
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
        return toResponse(
                actividad,
                requerimientos,
                profesionalesAsignados,
                estadoCronograma,
                null,
                null,
                false);
    }

    public static ActividadResponse toResponse(
            Actividad actividad,
            List<RequerimientoActividadResponse> requerimientos,
            List<ProfesionalAsignadoResponse> profesionalesAsignados,
            EstadoCronograma estadoCronograma,
            CalculoCpm calculo,
            LocalDateTime inicioProyeccion,
            boolean tentativa) {

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

        NodoCpm nodo = calculo == null || actividad.getIdActividad() == null
                ? null
                : calculo.nodos().get(actividad.getIdActividad());
        LocalDateTime inicioProyectado = nodo == null || inicioProyeccion == null
                ? null
                : inicioProyeccion.plusMinutes(nodo.inicioTemprano());
        LocalDateTime finProyectado = nodo == null || inicioProyeccion == null
                ? null
                : inicioProyeccion.plusMinutes(nodo.finTemprano());

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
                tentativa ? "true" : "false",
                nodo == null ? null : nodo.critica(),
                nodo == null ? null : nodo.holguraMakespan(),
                nodo == null ? null : nodo.holguraEntrega(),
                nodo == null ? null : nodo.ventanaInicio(),
                nodo == null ? null : nodo.ventanaEjecucion(),
                UbicacionMapper.toResponse(actividad.getUbicacion()),
                predecesoras,
                requerimientos != null ? requerimientos : List.of(),
                profesionales,
                inicioProyectado,
                finProyectado,
                calculo == null ? null : tentativa,
                nodo == null ? null : nodo.conflicto(),
                nodo == null ? null : nodo.motivoConflicto(),
                calculo == null ? null : calculo.parcial()
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

    public static ActividadPlanificacionResponse toPlanificacionResponse(
            Actividad actividad,
            List<ActividadVinculadaResponse> predecesoras,
            List<ProfesionalAsignadoResponse> responsables) {

        return new ActividadPlanificacionResponse(
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                actividad.getDuracionMinutos(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                actividad.getFechaHoraInicio(),
                actividad.getFechaHoraFin(),
                predecesoras,
                responsables);
    }

    public static ActividadCronogramaResponse toCronogramaResponse(
            Actividad actividad,
            NodoCpm nodo,
            LocalDateTime inicioProyeccion,
            boolean tentativo,
            List<ActividadVinculadaResponse> predecesoras) {

        LocalDateTime inicioProyectado = inicioProyeccion.plusMinutes(nodo.inicioTemprano());
        LocalDateTime finProyectado = inicioProyeccion.plusMinutes(nodo.finTemprano());
        LocalDateTime finConHolgura = inicioProyeccion.plusMinutes(nodo.finTardioMakespan());

        return new ActividadCronogramaResponse(
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                actividad.getDuracionMinutos(),
                predecesoras,
                inicioProyectado,
                finProyectado,
                finConHolgura,
                nodo.critica(),
                nodo.enRutaCritica(),
                nodo.holguraMakespan(),
                nodo.holguraEntrega(),
                nodo.ventanaInicio(),
                nodo.ventanaEjecucion(),
                nodo.conflicto(),
                nodo.motivoConflicto(),
                nodo.fijo(),
                tentativo);
    }
}