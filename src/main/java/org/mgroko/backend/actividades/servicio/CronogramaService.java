package org.mgroko.backend.actividades.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.dto.ProyeccionCpm;
import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.springframework.stereotype.Service;

/**
 * Cálculo del cronograma de un proyecto con el método de la ruta crítica.
 *
 * <p>El cálculo se divide en dos pasos independientes:</p>
 * <ol>
 *   <li>{@link #calcular(List)}: CPM <strong>relativo</strong>, en minutos
 *       desde el inicio del proyecto ({@code t0}). Es puro y determinista: no
 *       lee el reloj, no recibe fechas absolutas y no modifica las
 *       actividades.</li>
 *   <li>{@link #proyectar(CalculoCpm, LocalDateTime, boolean)}: traduce los
 *       offsets a fechas absolutas usando un {@code t0} explícito.</li>
 * </ol>
 *
 * <p>El {@code t0} se resuelve en {@link #inicioEfectivo(Planificacion)}:</p>
 * <ul>
 *   <li>si el proyecto tiene {@code fechaInicio} (se fija al confirmarlo), se
 *       usa esa fecha;</li>
 *   <li>si no la tiene, se usa el inicio del día actual como
 *       <strong>estimación</strong>. Las fechas así proyectadas son
 *       tentativas ({@link #esTentativa(Planificacion)}) y deben recalcularse
 *       cuando el proyecto se confirme;</li>
 *   <li>si la fecha de inicio quedó en el pasado, se replanifica desde hoy:
 *       no tiene sentido programar trabajo pendiente en el pasado.</li>
 * </ul>
 *
 * <p>La viabilidad se evalúa comparando el {@code makespan} con los minutos
 * disponibles hasta la fecha de entrega, no contra las fechas persistidas.
 * Por eso {@link #recalcular} mantiene su firma y su comportamiento
 * observable, pero ahora las fechas que escribe son una proyección del
 * cálculo relativo.</p>
 */
@Service
public class CronogramaService {

    private static final LocalTime HORA_FIN_CRONOGRAMA = LocalTime.of(23, 59);

    private final Clock reloj;

    public CronogramaService(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Recalcula y persiste las fechas de las actividades pendientes. Conserva
     * la firma histórica para no cambiar a sus llamadores.
     */
    public EstadoCronograma recalcular(Planificacion planificacion, List<Actividad> actividades) {
        return recalcular(planificacion, actividades, false);
    }

    /**
     * Igual que {@link #recalcular} pero no lanza excepción si el cronograma
     * desborda la fecha de entrega: devuelve {@code ALERTA_DESBORDE}.
     */
    public EstadoCronograma recalcularTolerandoDesborde(
            Planificacion planificacion,
            List<Actividad> actividades) {
        return recalcular(planificacion, actividades, true);
    }

    /**
     * Evalúa si el cronograma entra en la fecha de entrega sin modificar las
     * actividades. Si el proyecto no tiene fecha de entrega devuelve
     * {@code NORMAL}, igual que antes.
     */
    public EstadoCronograma evaluar(Planificacion planificacion, List<Actividad> actividades) {
        if (planificacion.getFechaEntrega() == null) {
            return EstadoCronograma.NORMAL;
        }

        LocalDateTime inicio = inicioEfectivo(planificacion);
        CalculoCpm calculo = calcular(actividades, iniciosFijos(actividades, inicio));

        return calculo.esViable(minutosHastaEntrega(planificacion, inicio))
                ? EstadoCronograma.NORMAL
                : EstadoCronograma.ALERTA_DESBORDE;
    }

    /**
     * CPM relativo para un conjunto de actividades programables. No usa el
     * reloj ni fechas persistidas: los offsets se calculan solo con duraciones
     * y dependencias.
     */
    public CalculoCpm calcular(List<Actividad> actividades) {
        return calcular(actividades, Map.of());
    }

    /**
     * CPM relativo con inicios fijos. Las actividades presentes en
     * {@code iniciosFijos} conservan ese offset (en minutos desde {@code t0})
     * en lugar de reprogramarse; se usa para actividades que ya están
     * {@code EN_CURSO} o {@code FINALIZADA}.
     *
     * @param iniciosFijos offset fijo por id de actividad, en minutos desde
     *                     {@code t0}
     */
    public CalculoCpm calcular(List<Actividad> actividades, Map<Long, Long> iniciosFijos) {
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

        Map<Long, Long> iniciosTempranos = new HashMap<>();
        Map<Long, Long> finesTempranos = new HashMap<>();
        List<Long> orden = new ArrayList<>();
        long makespan = 0;

        while (!cola.isEmpty()) {
            Actividad actual = cola.removeFirst();

            long inicio;
            Long inicioFijo = iniciosFijos.get(actual.getIdActividad());
            if (inicioFijo != null) {
                inicio = inicioFijo;
            } else {
                inicio = 0;
                for (Actividad predecesora : actual.getPredecesoras()) {
                    Long finPredecesora = finesTempranos.get(predecesora.getIdActividad());
                    if (finPredecesora != null && finPredecesora > inicio) {
                        inicio = finPredecesora;
                    }
                }
            }

            long fin = inicio + actual.getDuracionMinutos();
            iniciosTempranos.put(actual.getIdActividad(), inicio);
            finesTempranos.put(actual.getIdActividad(), fin);
            makespan = Math.max(makespan, fin);
            orden.add(actual.getIdActividad());

            for (Actividad sucesora : sucesoras.getOrDefault(
                    actual.getIdActividad(), List.of())) {
                int restantes = gradosEntrada.merge(
                        sucesora.getIdActividad(), -1, Integer::sum);
                if (restantes == 0) {
                    cola.addLast(sucesora);
                }
            }
        }

        if (orden.size() != actividades.size()) {
            throw new CronogramaInviableException(
                    "El cronograma contiene un ciclo entre actividades.");
        }

        Map<Long, Long> iniciosTardios = new HashMap<>();
        Map<Long, NodoCpm> nodos = new LinkedHashMap<>();

        for (int i = orden.size() - 1; i >= 0; i--) {
            Long id = orden.get(i);
            Actividad actividad = actividadesPorId.get(id);

            long finTardio = makespan;
            for (Actividad sucesora : sucesoras.getOrDefault(id, List.of())) {
                Long inicioTardioSucesora = iniciosTardios.get(sucesora.getIdActividad());
                if (inicioTardioSucesora != null && inicioTardioSucesora < finTardio) {
                    finTardio = inicioTardioSucesora;
                }
            }

            long inicioTardio = finTardio - actividad.getDuracionMinutos();
            iniciosTardios.put(id, inicioTardio);

            long inicioTemprano = iniciosTempranos.get(id);
            long finTemprano = finesTempranos.get(id);
            long holgura = inicioTardio - inicioTemprano;

            nodos.put(id, new NodoCpm(
                    id,
                    actividad.getDuracionMinutos(),
                    inicioTemprano,
                    finTemprano,
                    inicioTardio,
                    finTardio,
                    holgura,
                    holgura <= 0,
                    iniciosFijos.containsKey(id)));
        }

        return new CalculoCpm(nodos, makespan);
    }

    /**
     * Traduce un cálculo relativo a fechas absolutas.
     *
     * @param calculo   resultado de {@link #calcular(List)}
     * @param inicio    {@code t0} a usar como origen
     * @param tentativa indica si {@code inicio} es estimado
     */
    public ProyeccionCpm proyectar(
            CalculoCpm calculo,
            LocalDateTime inicio,
            boolean tentativa) {

        Map<Long, LocalDateTime> fechas = new LinkedHashMap<>();
        for (NodoCpm nodo : calculo.nodos().values()) {
            fechas.put(nodo.idActividad(), inicio.plusMinutes(nodo.inicioTemprano()));
        }
        return new ProyeccionCpm(inicio, tentativa, fechas);
    }

    /**
     * Origen del cálculo para el proyecto: su fecha de inicio confirmada o,
     * mientras no exista, el inicio del día actual. Si la fecha de inicio ya
     * pasó, se replanifica desde hoy.
     */
    public LocalDateTime inicioEfectivo(Planificacion planificacion) {
        LocalDateTime inicioDelDia = LocalDate.now(reloj).atStartOfDay();
        LocalDateTime fechaInicio = planificacion.getProyecto() != null
                ? planificacion.getProyecto().getFechaInicio()
                : null;

        if (fechaInicio == null || fechaInicio.isBefore(inicioDelDia)) {
            return inicioDelDia;
        }
        return fechaInicio;
    }

    /**
     * Indica si las fechas del proyecto son una estimación porque todavía no
     * se confirmó su fecha de inicio.
     */
    public boolean esTentativa(Planificacion planificacion) {
        return planificacion.getProyecto() == null
                || planificacion.getProyecto().getFechaInicio() == null;
    }

    private EstadoCronograma recalcular(
            Planificacion planificacion,
            List<Actividad> actividades,
            boolean tolerarDesborde) {

        LocalDateTime finCronograma = finCronograma(planificacion);
        LocalDateTime inicio = inicioEfectivo(planificacion);
        CalculoCpm calculo = calcular(actividades, iniciosFijos(actividades, inicio));

        long minutosDisponibles = Duration.between(inicio, finCronograma).toMinutes();
        boolean desborde = !calculo.esViable(minutosDisponibles);

        if (desborde && !tolerarDesborde) {
            throw new CronogramaInviableException(
                    "El cronograma no es viable: la fecha de fin calculada supera la fecha de entrega.");
        }

        ProyeccionCpm proyeccion = proyectar(
                calculo, inicio, esTentativa(planificacion));

        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE) {
                actividad.setFechaHoraInicio(proyeccion.fechaDe(actividad.getIdActividad()));
            }
        }

        return desborde ? EstadoCronograma.ALERTA_DESBORDE : EstadoCronograma.NORMAL;
    }

    /**
     * Offset fijo de las actividades que no están {@code PENDIENTE}. Si una
     * actividad no pendiente no tiene fecha, se fija en {@code t0}, igual que
     * el comportamiento anterior.
     */
    private Map<Long, Long> iniciosFijos(
            List<Actividad> actividades,
            LocalDateTime inicio) {

        Map<Long, Long> fijos = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE) {
                continue;
            }
            LocalDateTime fecha = actividad.getFechaHoraInicio() != null
                    ? actividad.getFechaHoraInicio()
                    : inicio;
            fijos.put(
                    actividad.getIdActividad(),
                    Duration.between(inicio, fecha).toMinutes());
        }
        return fijos;
    }

    private LocalDateTime finCronograma(Planificacion planificacion) {
        if (planificacion.getFechaEntrega() == null) {
            throw new FechaEntregaNoDefinidaException();
        }
        return planificacion.getFechaEntrega().atTime(HORA_FIN_CRONOGRAMA);
    }

    private long minutosHastaEntrega(Planificacion planificacion, LocalDateTime inicio) {
        return Duration.between(inicio, finCronograma(planificacion)).toMinutes();
    }
}
