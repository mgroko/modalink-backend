package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.mgroko.backend.modelo.Actividad;

/**
 * Orden estable de actividades para las vistas de planificación y cronograma.
 *
 * <p>Usa Kahn sobre el grafo de dependencias con desempate por id de actividad.
 * No necesita CPM ni fechas: la tabla de Planificación es liviana y el
 * cronograma usa el mismo orden para que ambas pestañas coincidan.</p>
 *
 * <p>Si hubiera un ciclo (dato inválido), las actividades restantes se agregan
 * por id para no romper la lectura.</p>
 */
public final class OrdenActividades {

    private OrdenActividades() {
    }

    public static List<Actividad> topologico(List<Actividad> actividades) {
        if (actividades == null || actividades.isEmpty()) {
            return List.of();
        }

        Map<Long, Actividad> porId = new LinkedHashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getIdActividad() != null) {
                porId.put(actividad.getIdActividad(), actividad);
            }
        }
        if (porId.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> grados = new HashMap<>();
        Map<Long, List<Long>> sucesoras = new HashMap<>();
        for (Long id : porId.keySet()) {
            grados.put(id, 0);
            sucesoras.put(id, new ArrayList<>());
        }

        for (Actividad actividad : porId.values()) {
            for (Actividad predecesora : actividad.getPredecesoras()) {
                Long idPredecesora = predecesora.getIdActividad();
                if (idPredecesora == null || !porId.containsKey(idPredecesora)) {
                    continue;
                }
                grados.merge(actividad.getIdActividad(), 1, Integer::sum);
                sucesoras.get(idPredecesora).add(actividad.getIdActividad());
            }
        }

        PriorityQueue<Long> libres = new PriorityQueue<>();
        grados.forEach((id, grado) -> {
            if (grado == 0) {
                libres.add(id);
            }
        });

        List<Actividad> ordenadas = new ArrayList<>(porId.size());
        Set<Long> visitadas = new HashSet<>();
        while (!libres.isEmpty()) {
            Long id = libres.poll();
            if (!visitadas.add(id)) {
                continue;
            }
            ordenadas.add(porId.get(id));
            for (Long sucesora : sucesoras.getOrDefault(id, List.of())) {
                int grado = grados.merge(sucesora, -1, Integer::sum);
                if (grado == 0) {
                    libres.add(sucesora);
                }
            }
        }

        if (ordenadas.size() < porId.size()) {
            porId.values().stream()
                    .filter(actividad -> !visitadas.contains(actividad.getIdActividad()))
                    .sorted(Comparator.comparing(
                            Actividad::getIdActividad,
                            Comparator.nullsLast(Long::compareTo)))
                    .forEach(ordenadas::add);
        }

        return ordenadas;
    }
}
