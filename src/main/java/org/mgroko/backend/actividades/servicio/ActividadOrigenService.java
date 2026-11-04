package org.mgroko.backend.actividades.servicio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.springframework.stereotype.Service;

/**
 * Calcula el origen efectivo de una o varias actividades.
 *
 * <p>Resuelve en lote las asignaciones activas para evitar una consulta por
 * fila. El filtro de requerimientos archivados se agregará cuando el historial
 * incorpore {@code fecha_archivado}.</p>
 */
@Service
public class ActividadOrigenService {

    private final AsignacionActividadRepository asignacionActividadRepository;

    public ActividadOrigenService(
            AsignacionActividadRepository asignacionActividadRepository) {
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    public OrigenInicio calcular(Actividad actividad) {
        if (actividad == null || actividad.getIdActividad() == null) {
            return OrigenInicioResolver.resolver(
                    actividad, proyectoDe(actividad), false);
        }

        boolean tieneAsignacionesActivas = asignacionActividadRepository
                .existsByActividad_IdActividadAndMiembro_EstadoParticipacion(
                        actividad.getIdActividad(),
                        EstadoParticipacion.ACTIVO);
        return OrigenInicioResolver.resolver(
                actividad, proyectoDe(actividad), tieneAsignacionesActivas);
    }

    /**
     * Calcula el origen de un conjunto de actividades con una sola consulta.
     *
     * @return mapa idActividad → origen; las actividades sin id se omiten
     */
    public Map<Long, OrigenInicio> calcular(Collection<Actividad> actividades) {
        if (actividades == null || actividades.isEmpty()) {
            return Map.of();
        }

        Set<Long> ids = actividades.stream()
                .filter(Objects::nonNull)
                .map(Actividad::getIdActividad)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> conAsignacionesActivas = ids.isEmpty()
                ? Set.of()
                : asignacionActividadRepository
                        .findIdsActividadesConAsignacionesActivas(ids);

        Map<Long, OrigenInicio> resultado = new LinkedHashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad == null || actividad.getIdActividad() == null) {
                continue;
            }
            resultado.put(
                    actividad.getIdActividad(),
                    OrigenInicioResolver.resolver(
                            actividad,
                            proyectoDe(actividad),
                            conAsignacionesActivas.contains(
                                    actividad.getIdActividad())));
        }
        return resultado;
    }

    private Proyecto proyectoDe(Actividad actividad) {
        return actividad != null && actividad.getPlanificacion() != null
                ? actividad.getPlanificacion().getProyecto()
                : null;
    }
}
