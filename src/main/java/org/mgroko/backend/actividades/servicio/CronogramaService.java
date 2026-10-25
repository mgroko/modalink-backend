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
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.springframework.stereotype.Service;

@Service
public class CronogramaService {

    private static final LocalTime HORA_FIN_CRONOGRAMA = LocalTime.of(23, 59);

    public EstadoCronograma recalcular(Planificacion planificacion, List<Actividad> actividades) {
        return recalcular(planificacion, actividades, false);
    }

    public EstadoCronograma recalcularTolerandoDesborde(
            Planificacion planificacion,
            List<Actividad> actividades) {
        return recalcular(planificacion, actividades, true);
    }

    public EstadoCronograma evaluar(Planificacion planificacion, List<Actividad> actividades) {
        LocalDate fechaEntrega = planificacion.getFechaEntrega();
        if (fechaEntrega == null) {
            return EstadoCronograma.NORMAL;
        }

        LocalDateTime finCronograma = fechaEntrega.atTime(HORA_FIN_CRONOGRAMA);

        boolean desborde = actividades.stream().anyMatch(actividad -> {
            LocalDateTime fin = actividad.getFechaHoraFin();
            return fin != null && fin.isAfter(finCronograma);
        });

        return desborde ? EstadoCronograma.ALERTA_DESBORDE : EstadoCronograma.NORMAL;
    }

    private EstadoCronograma recalcular(
            Planificacion planificacion,
            List<Actividad> actividades,
            boolean tolerarDesborde) {

        LocalDate fechaEntrega = planificacion.getFechaEntrega();
        if (fechaEntrega == null) {
            throw new FechaEntregaNoDefinidaException();
        }

        LocalDateTime finCronograma = fechaEntrega.atTime(HORA_FIN_CRONOGRAMA);
        LocalDateTime inicioBase = inicioBase(planificacion);

        Map<Long, Actividad> actividadesPorId = actividades.stream()
                .collect(Collectors.toMap(Actividad::getIdActividad, Function.identity()));

        Map<Long, List<Actividad>> sucesoras = new HashMap<>();
        Map<Long, Integer> gradosEntrada = new HashMap<>();
        for (Actividad actividad : actividades) {
            gradosEntrada.put(actividad.getIdActividad(), 0);
        }

        for (Actividad actividad : actividades) {
            for (Actividad predecesora : actividad.getPredecesoras()) {
                if (!actividadesPorId.containsKey(predecesora.getIdActividad())) {
                    continue;
                }
                sucesoras.computeIfAbsent(
                        predecesora.getIdActividad(),
                        id -> new ArrayList<>())
                        .add(actividad);
                gradosEntrada.merge(actividad.getIdActividad(), 1, Integer::sum);
            }
        }

        Deque<Actividad> cola = new ArrayDeque<>();
        for (Actividad actividad : actividades) {
            if (gradosEntrada.get(actividad.getIdActividad()) == 0) {
                cola.addLast(actividad);
            }
        }

        Map<Long, LocalDateTime> finesTempranos = new HashMap<>();
        List<Long> orden = new ArrayList<>();
        int procesadas = 0;

        while (!cola.isEmpty()) {
            Actividad actual = cola.removeFirst();
            LocalDateTime inicio;

            if (actual.getEstado() == EstadoActividad.PENDIENTE) {
                inicio = inicioBase;
                for (Actividad predecesora : actual.getPredecesoras()) {
                    LocalDateTime finPredecesora = finesTempranos.get(
                            predecesora.getIdActividad());
                    if (finPredecesora != null && finPredecesora.isAfter(inicio)) {
                        inicio = finPredecesora;
                    }
                }
                actual.setFechaHoraInicio(inicio);
            } else {
                inicio = actual.getFechaHoraInicio() != null
                        ? actual.getFechaHoraInicio()
                        : inicioBase;
            }

            finesTempranos.put(
                    actual.getIdActividad(),
                    inicio.plusMinutes(actual.getDuracionMinutos()));
            orden.add(actual.getIdActividad());
            procesadas++;

            for (Actividad sucesora : sucesoras.getOrDefault(
                    actual.getIdActividad(), List.of())) {
                int restantes = gradosEntrada.merge(
                        sucesora.getIdActividad(), -1, Integer::sum);
                if (restantes == 0) {
                    cola.addLast(sucesora);
                }
            }
        }

        if (procesadas != actividades.size()) {
            throw new CronogramaInviableException(
                    "El cronograma contiene un ciclo entre actividades.");
        }

        Map<Long, LocalDateTime> iniciosTardios = calcularIniciosTardios(
                actividadesPorId, sucesoras, orden, finCronograma);

        boolean superaFechaEntrega = finesTempranos.values().stream()
                .anyMatch(fin -> fin.isAfter(finCronograma));
        boolean requiereInicioAnterior = iniciosTardios.values().stream()
                .anyMatch(inicio -> inicio.isBefore(inicioBase));
        boolean desborde = superaFechaEntrega || requiereInicioAnterior;

        if (desborde && !tolerarDesborde) {
            throw new CronogramaInviableException(
                    "El cronograma no es viable: la fecha de fin calculada supera la fecha de entrega.");
        }

        return desborde ? EstadoCronograma.ALERTA_DESBORDE : EstadoCronograma.NORMAL;
    }

    private LocalDateTime inicioBase(Planificacion planificacion) {
        LocalDateTime fechaInicio = planificacion.getProyecto().getFechaInicio();
        return fechaInicio != null ? fechaInicio : LocalDate.now().atStartOfDay();
    }

    private Map<Long, LocalDateTime> calcularIniciosTardios(
            Map<Long, Actividad> actividadesPorId,
            Map<Long, List<Actividad>> sucesoras,
            List<Long> orden,
            LocalDateTime finCronograma) {

        Map<Long, LocalDateTime> iniciosTardios = new HashMap<>();

        for (int i = orden.size() - 1; i >= 0; i--) {
            Long id = orden.get(i);
            Actividad actividad = actividadesPorId.get(id);

            LocalDateTime finTardio = finCronograma;
            List<Actividad> sucesorasActuales = sucesoras.getOrDefault(id, List.of());
            if (!sucesorasActuales.isEmpty()) {
                finTardio = sucesorasActuales.stream()
                        .map(sucesora -> iniciosTardios.get(sucesora.getIdActividad()))
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(finCronograma);
            }

            iniciosTardios.put(
                    id,
                    finTardio.minusMinutes(actividad.getDuracionMinutos()));
        }

        return iniciosTardios;
    }
}
