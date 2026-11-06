package org.mgroko.backend.actividades.servicio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.repositorio.ActividadRepository;

/**
 * Reconexión de dependencias cuando una actividad sale del cronograma.
 *
 * <p>Se usa tanto en la baja física como en la cancelación: las sucesoras de
 * la actividad heredan sus predecesoras. Así el grafo no conserva aristas
 * hacia actividades que ya no se ejecutan.</p>
 */
public final class ActividadDependencias {

    private ActividadDependencias() {
    }

    public static void reconectarSucesoras(
            ActividadRepository actividadRepository,
            Actividad actividad,
            List<Actividad> actividades) {

        List<Actividad> sucesoras = actividades.stream()
                .filter(candidata -> !Objects.equals(
                        candidata.getIdActividad(), actividad.getIdActividad()))
                .filter(candidata -> candidata.getPredecesoras().stream()
                        .anyMatch(predecesora -> Objects.equals(
                                predecesora.getIdActividad(), actividad.getIdActividad())))
                .toList();

        if (sucesoras.isEmpty()) {
            return;
        }

        Set<Actividad> predecesoras = new LinkedHashSet<>(actividad.getPredecesoras());
        for (Actividad sucesora : sucesoras) {
            sucesora.getPredecesoras().removeIf(predecesora -> Objects.equals(
                    predecesora.getIdActividad(), actividad.getIdActividad()));
            sucesora.getPredecesoras().addAll(predecesoras);
        }

        actividadRepository.flush();
    }
}
