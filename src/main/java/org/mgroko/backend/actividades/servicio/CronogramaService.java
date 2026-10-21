package org.mgroko.backend.actividades.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.springframework.stereotype.Service;

@Service
public class CronogramaService {

    private static final LocalTime HORA_FIN_CRONOGRAMA = LocalTime.of(23, 59);

    public void recalcular(Planificacion planificacion, List<Actividad> actividades) {
        LocalDate fechaEntrega = planificacion.getFechaEntrega();
        if (fechaEntrega == null) {
            throw new FechaEntregaNoDefinidaException();
        }

        LocalDateTime finCronograma = fechaEntrega.atTime(HORA_FIN_CRONOGRAMA);

        Map<Long, Actividad> actividadesPorId = actividades.stream()
                .collect(Collectors.toMap(Actividad::getIdActividad, Function.identity()));

        Map<Long, List<Actividad>> sucesoras = new HashMap<>();
        for (Actividad actividad : actividades) {
            for (Actividad predecesora : actividad.getPredecesoras()) {
                if (actividadesPorId.containsKey(predecesora.getIdActividad())) {
                    sucesoras.computeIfAbsent(
                            predecesora.getIdActividad(),
                            id -> new ArrayList<>())
                            .add(actividad);
                }
            }
        }

        Map<Long, Actividad> pendientes = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.PENDIENTE)
                .collect(Collectors.toMap(Actividad::getIdActividad, Function.identity()));

        Map<Long, Integer> sucesorasPendientes = new HashMap<>();
        for (Actividad pendiente : pendientes.values()) {
            int cantidad = (int) sucesoras.getOrDefault(pendiente.getIdActividad(), List.of()).stream()
                    .filter(sucesora -> sucesora.getEstado() == EstadoActividad.PENDIENTE)
                    .count();
            sucesorasPendientes.put(pendiente.getIdActividad(), cantidad);
        }

        Deque<Actividad> cola = new ArrayDeque<>();
        for (Actividad pendiente : pendientes.values()) {
            if (sucesorasPendientes.get(pendiente.getIdActividad()) == 0) {
                cola.addLast(pendiente);
            }
        }

        Map<Long, LocalDateTime> iniciosCalculados = new HashMap<>();
        int procesadas = 0;

        while (!cola.isEmpty()) {
            Actividad actual = cola.removeFirst();
            List<Actividad> sucesorasActuales = sucesoras.getOrDefault(
                    actual.getIdActividad(), List.of());

            LocalDateTime fin;
            if (sucesorasActuales.isEmpty()) {
                fin = finCronograma;
            } else {
                fin = sucesorasActuales.stream()
                        .map(sucesora -> inicioDe(sucesora, iniciosCalculados))
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(finCronograma);
            }

            iniciosCalculados.put(
                    actual.getIdActividad(),
                    fin.minusMinutes(actual.getDuracionMinutos()));
            procesadas++;

            for (Actividad predecesora : actual.getPredecesoras()) {
                if (!pendientes.containsKey(predecesora.getIdActividad())) {
                    continue;
                }
                int restantes = sucesorasPendientes.merge(
                        predecesora.getIdActividad(), -1, Integer::sum);
                if (restantes == 0) {
                    cola.addLast(predecesora);
                }
            }
        }

        if (procesadas != pendientes.size()) {
            throw new CronogramaInviableException(
                    "El cronograma contiene un ciclo entre actividades pendientes.");
        }

        LocalDateTime fechaInicioProyecto = planificacion.getProyecto().getFechaInicio();
        if (fechaInicioProyecto != null) {
            for (Actividad pendiente : pendientes.values()) {
                LocalDateTime inicio = iniciosCalculados.get(pendiente.getIdActividad());
                if (inicio.isBefore(fechaInicioProyecto)) {
                    throw new CronogramaInviableException(
                            "El cronograma no es viable: la actividad '" + pendiente.getNombre()
                                    + "' comenzaria antes del inicio del proyecto.");
                }
            }
        }

        for (Actividad pendiente : pendientes.values()) {
            pendiente.setFechaHoraInicio(
                    iniciosCalculados.get(pendiente.getIdActividad()));
        }
    }

    private LocalDateTime inicioDe(
            Actividad actividad,
            Map<Long, LocalDateTime> iniciosCalculados) {
        if (actividad.getEstado() == EstadoActividad.PENDIENTE) {
            return iniciosCalculados.get(actividad.getIdActividad());
        }
        return actividad.getFechaHoraInicio();
    }
}
