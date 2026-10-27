package org.mgroko.backend.actividades.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.ConflictoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.dto.ProyeccionCpm;
import org.mgroko.backend.actividades.dto.RangoFactibleAbsoluto;
import org.mgroko.backend.actividades.exception.CronogramaInviableException;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.actividades.dto.RangoFactible;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.springframework.stereotype.Service;

/**
 * Cálculo del cronograma de un proyecto con el método de la ruta crítica.
 *
 * <p>El cálculo se divide en dos pasos independientes:</p>
 * <ol>
 *   <li>{@link #calcular(List, Map, Long, long)}: CPM <strong>relativo</strong>,
 *       en minutos desde el inicio del proyecto ({@code t0}). Es puro y
 *       determinista: no lee el reloj, no recibe fechas absolutas y no modifica
 *       las actividades.</li>
 *   <li>{@link #proyectar(CalculoCpm, LocalDateTime, boolean)}: traduce los
 *       offsets a fechas absolutas usando un {@code t0} explícito.</li>
 * </ol>
 *
 * <p>El motor ejecuta <strong>dos pasadas hacia atrás</strong> (plan v3,
 * sección 3):</p>
 * <ul>
 *   <li><strong>makespan</strong>: ancla los terminales en el makespan y
 *       calcula la ruta crítica y la holgura propia del grafo;</li>
 *   <li><strong>entrega</strong>: ancla los terminales libres en {@code T} y
 *       calcula los últimos inicios y fines reales, propagando el mínimo entre
 *       sucesoras.</li>
 * </ul>
 *
 * <p>Las actividades fijas se anclan en ambas pasadas ({@code LS = ES},
 * {@code LF = EF}). El margen global {@code T - makespan} se informa como
 * indicador, pero <strong>nunca se suma a un LF</strong>.</p>
 *
 * <p>El {@code t0} se resuelve en {@link #inicioEfectivo(Planificacion)}:</p>
 * <ul>
 *   <li>si el proyecto tiene {@code fechaInicio} (se fija al confirmarlo), se
 *       usa esa fecha;</li>
 *   <li>si no la tiene, se usa el inicio del día actual como
 *       <strong>estimación</strong>. Las fechas así proyectadas son
 *       tentativas ({@link #esTentativa(Planificacion)}) y deben recalcularse
 *       cuando el proyecto se confirme;</li>
 * <li>si la fecha de inicio quedó en el pasado, se conserva como {@code t0};
 *       las actividades libres usan un {@code releaseTime} separado.</li>
 * </ul>
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
     *
     * <p>La fase 2 elimina la persistencia de proyecciones; mientras tanto este
     * método mantiene el comportamiento anterior.</p>
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
     * actividades.
     *
     * <p>Si el proyecto no tiene fecha de entrega devuelve {@code SIN_FECHA_ENTREGA}
     * (D-09/D-11 del plan v3). La confirmación rechazara con {@code 409} hasta que
     * exista entrega.</p>
     */
    public EstadoCronograma evaluar(Planificacion planificacion, List<Actividad> actividades) {
        if (planificacion.getFechaEntrega() == null) {
            return EstadoCronograma.SIN_FECHA_ENTREGA;
        }

        CalculoCpm calculo = calcularProyeccion(planificacion, actividades);
        return !Boolean.TRUE.equals(calculo.viable())
                ? EstadoCronograma.ALERTA_DESBORDE
                : EstadoCronograma.NORMAL;
    }

    /**
     * CPM relativo para un conjunto de actividades sin inicios fijos y sin
     * fecha de entrega. Es la fachada que conserva la firma histórica y sirve
     * para grafos puros de test.
     */
    public CalculoCpm calcular(List<Actividad> actividades) {
        return calcular(actividades, Map.of(), null, 0L);
    }

    /**
     * CPM relativo con inicios fijos. Las actividades presentes en
     * {@code iniciosFijos} conservan ese offset (en minutos desde {@code t0})
     * en lugar de reprogramarse; se usa para actividades que ya están
     * {@code EN_CURSO}, {@code FINALIZADA} o con inicio confirmado.
     *
     * @param iniciosFijos offset fijo por id de actividad, en minutos desde
     *                     {@code t0}
     */
    public CalculoCpm calcular(List<Actividad> actividades, Map<Long, Long> iniciosFijos) {
        return calcular(actividades, iniciosFijos, null, 0L);
    }

    /**
     * CPM relativo de todas las actividades de una planificación, resolviendo
     * el {@code t0} del proyecto y fijando las actividades que no están
     * {@code PENDIENTE}. Es la variante que usan los casos de uso que
     * necesitan el rango factible de una actividad concreta (UC-26, UC-70).
     *
     * <p>No pasa la fecha de entrega al motor: la lectura de rangos sigue
     * usando la pasada de makespan hasta que la fase 2/3 introduzca
     * {@code releaseTime} y disponibilidad multi-miembro.</p>
     *
     * @param planificacion planificación del proyecto
     * @param actividades   actividades de la planificación
     * @return cálculo relativo con los offsets ya fijados
     */
    public CalculoCpm calcular(Planificacion planificacion, List<Actividad> actividades) {
        return calcularProyeccion(planificacion, actividades);
    }

    /**
     * Calcula el CPM que consumen las respuestas de lectura. El origen del
     * proyecto es estable; el reloj solo aporta la restricción de no colocar
     * actividades nuevas en el pasado.
     */
    public CalculoCpm calcularProyeccion(
            Planificacion planificacion,
            List<Actividad> actividades) {
        LocalDateTime inicio = inicioEfectivo(planificacion);
        Long entrega = planificacion.getFechaEntrega() == null
                ? null
                : minutosHastaEntrega(planificacion, inicio);
        long releaseTime = Math.max(
                0L,
                Duration.between(inicio, LocalDateTime.now(reloj)).toMinutes());
        return calcular(
                actividades,
                iniciosFijos(actividades, inicio),
                entrega,
                releaseTime);
    }

    /**
     * Motor CPM puro de dos pasadas (plan v3, sección 3).
     *
     * <p>No lee el reloj ni la base de datos y no modifica las actividades. El
     * {@code t0} queda fuera del cálculo: todos los instantes son offsets en
     * minutos desde él, y el llamador es responsable de traducirlos con
     * {@link #proyectar}. Las actividades {@code CANCELADA} se excluyen del
     * grafo.</p>
     *
     * @param actividades         actividades del proyecto (pueden incluir
     *                            canceladas)
     * @param iniciosFijos        inicio fijo {@code S_i} en minutos desde
     *                            {@code t0}, por id de actividad; las
     *                            actividades ausentes se programan libremente
     * @param minutosHastaEntrega {@code T} (minutos entre {@code t0} y el fin
     *                            de la entrega); {@code null} si no hay fecha
     *                            de entrega
     * @param releaseTime         cota inferior {@code R >= 0} para el inicio de
     *                            las actividades libres; no mueve {@code t0} ni
     *                            las actividades fijas
     * @return resultado con ambas holguras, ventanas, conflictos y viabilidad
     * @throws CronogramaInviableException si hay un ciclo o una duración
     *                                     inválida
     */
    public CalculoCpm calcular(
            List<Actividad> actividades,
            Map<Long, Long> iniciosFijos,
            Long minutosHastaEntrega,
            long releaseTime) {

        if (releaseTime < 0) {
            throw new IllegalArgumentException("El release time no puede ser negativo.");
        }

        Grafo grafo = construirGrafo(actividades);
        Map<Long, Long> iniciosTempranos = new LinkedHashMap<>();
        Map<Long, Long> finesTempranos = new LinkedHashMap<>();
        pasadaAdelante(grafo, iniciosFijos, releaseTime, iniciosTempranos, finesTempranos);

        long makespan = finesTempranos.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        Map<Long, Long> iniciosTardiosMakespan = new HashMap<>();
        Map<Long, Long> finesTardiosMakespan = new HashMap<>();
        Map<Long, Long> iniciosTardiosEntrega = new HashMap<>();
        Map<Long, Long> finesTardiosEntrega = new HashMap<>();
        pasadaAtras(
                grafo,
                iniciosFijos,
                minutosHastaEntrega,
                iniciosTempranos,
                finesTempranos,
                makespan,
                iniciosTardiosMakespan,
                finesTardiosMakespan,
                iniciosTardiosEntrega,
                finesTardiosEntrega);

        List<ConflictoCpm> conflictos = new ArrayList<>();
        Map<Long, List<String>> motivos = detectarConflictos(
                grafo, iniciosFijos, finesTempranos, conflictos);

        Set<Long> enRutaCritica = marcarRutaCritica(
                grafo, iniciosTempranos, finesTempranos, makespan);

        Map<Long, NodoCpm> nodos = new LinkedHashMap<>();
        for (Long id : grafo.orden()) {
            Actividad actividad = grafo.porId().get(id);
            Long inicioFijo = iniciosFijos.get(id);
            boolean fijo = inicioFijo != null;

            long holguraMakespan = iniciosTardiosMakespan.get(id) - iniciosTempranos.get(id);
            Long holguraEntrega = minutosHastaEntrega == null
                    ? null
                    : iniciosTardiosEntrega.get(id) - iniciosTempranos.get(id);

            String motivo = motivos.containsKey(id)
                    ? String.join("; ", motivos.get(id))
                    : motivoHolgura(holguraMakespan, holguraEntrega);

            nodos.put(id, new NodoCpm(
                    id,
                    duracionValida(actividad),
                    fijo,
                    inicioFijo,
                    iniciosTempranos.get(id),
                    finesTempranos.get(id),
                    iniciosTardiosMakespan.get(id),
                    finesTardiosMakespan.get(id),
                    holguraMakespan,
                    holguraMakespan == 0,
                    minutosHastaEntrega == null ? null : iniciosTardiosEntrega.get(id),
                    minutosHastaEntrega == null ? null : finesTardiosEntrega.get(id),
                    holguraEntrega,
                    motivo != null,
                    motivo,
                    enRutaCritica.contains(id)));
        }

        Boolean viable = null;
        if (minutosHastaEntrega != null) {
            boolean holgurasNoNegativas = nodos.values().stream()
                    .allMatch(nodo -> nodo.holguraMakespan() >= 0
                            && (nodo.holguraEntrega() == null || nodo.holguraEntrega() >= 0));
            viable = makespan <= minutosHastaEntrega
                    && conflictos.isEmpty()
                    && holgurasNoNegativas;
        }

        return new CalculoCpm(nodos, makespan, minutosHastaEntrega, viable, conflictos);
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
     * Proyecta el rango factible de una actividad a fechas absolutas.
     *
     * <p>Para una actividad pendiente devuelve {@code [ES, LF]}. Para una
     * actividad con fecha fija devuelve su ventana realmente agendada
     * ({@code [ES, EF]}), porque no tiene sentido evaluar disponibilidad sobre
     * un margen que ya no puede usarse.</p>
     *
     * <p>Cuando el proyecto todavía no tiene {@code fechaInicio}, el origen es
     * una estimación (inicio del día) y {@code [ES, LF]} puede caer fuera de
     * la jornada laboral de cualquier persona, lo que bloquearía postulaciones
     * legítimas antes de UC-60. En ese caso el rango se amplía a
     * {@code [ahora, fecha de entrega]}: la actividad todavía no está agendada
     * y lo único que se puede afirmar es que hay que ejecutarla antes del
     * plazo. El resultado se marca como tentativo y debe re-verificarse al
     * confirmar el proyecto (UC-60) y al asignar la actividad (UC-70).</p>
     *
     * @param planificacion planificación del proyecto
     * @param calculo       cálculo relativo de la planificación
     * @param idActividad   actividad cuyo rango se quiere proyectar
     * @return rango absoluto y bandera de tentatividad
     */
    public RangoFactibleAbsoluto rangoFactibleAbsoluto(
            Planificacion planificacion,
            CalculoCpm calculo,
            Long idActividad) {

        LocalDateTime inicio = inicioEfectivo(planificacion);
        NodoCpm nodo = calculo.nodoDe(idActividad);
        RangoFactible ventanaInicio = nodo.ventanaInicio();
        RangoFactible ventanaEjecucion = nodo.ventanaEjecucion();
        LocalDateTime fechaInicioMinima = inicio.plusMinutes(ventanaInicio.inicioMinutos());
        LocalDateTime fechaInicioMaxima = inicio.plusMinutes(ventanaInicio.finMinutos());
        LocalDateTime fechaFinMinima = inicio.plusMinutes(nodo.finTemprano());
        LocalDateTime fechaFinMaxima = inicio.plusMinutes(ventanaEjecucion.finMinutos());
        boolean tentativa = esTentativa(planificacion);
        boolean parcial = calculo.parcial();

        if (tentativa) {
            LocalDateTime ahora = LocalDateTime.now(reloj);
            if (planificacion.getFechaEntrega() != null) {
                fechaFinMaxima = planificacion.getFechaEntrega().atTime(HORA_FIN_CRONOGRAMA);
            }
            if (fechaInicioMinima.isBefore(ahora)) {
                fechaInicioMinima = ahora;
            }
            if (fechaInicioMaxima.isBefore(fechaInicioMinima)) {
                fechaInicioMaxima = fechaInicioMinima;
            }
            if (fechaFinMinima.isBefore(fechaInicioMinima)) {
                fechaFinMinima = fechaInicioMinima;
            }
            if (fechaFinMaxima.isBefore(fechaFinMinima)) {
                fechaFinMaxima = fechaFinMinima;
            }
        }

        return new RangoFactibleAbsoluto(
                fechaInicioMinima,
                fechaInicioMaxima,
                fechaFinMinima,
                fechaFinMaxima,
                nodo.fijo(),
                tentativa,
                parcial,
                nodo.conflicto() ? nodo.motivoConflicto() : null);
    }

    /**
     * Origen del cálculo para el proyecto: su fecha de inicio confirmada o,
     * mientras no exista, el inicio del día actual. Si la fecha de inicio ya
     * pasó, se replanifica desde hoy. La fase 2 corrige este comportamiento:
     * {@code t0} será inmutable y el “no programar en el pasado” se modelará
     * con un {@code releaseTime} separado.
     */
    public LocalDateTime inicioEfectivo(Planificacion planificacion) {
        LocalDateTime fechaInicio = planificacion.getProyecto() != null
                ? planificacion.getProyecto().getFechaInicio()
                : null;

        return fechaInicio != null
                ? fechaInicio
                : LocalDateTime.now(reloj).toLocalDate().atStartOfDay();
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
        long minutosDisponibles = Duration.between(inicio, finCronograma).toMinutes();
        long releaseTime = Math.max(
                0L,
                Duration.between(inicio, LocalDateTime.now(reloj)).toMinutes());
        CalculoCpm calculo = calcular(
                actividades,
                iniciosFijos(actividades, inicio),
                minutosDisponibles,
                releaseTime);

        boolean desborde = calculo.makespan() > minutosDisponibles;

        if (desborde && !tolerarDesborde) {
            throw new CronogramaInviableException(
                    "El cronograma no es viable: la fecha de fin calculada supera la fecha de entrega.");
        }

        // Fase 2: solo cálculo de lectura, no persistir proyecciones en PENDIENTE
        ProyeccionCpm proyeccion = proyectar(
                calculo, inicio, esTentativa(planificacion));

        // No se escribe fechaHoraInicio en actividades PENDIENTE:
        // Las proyecciones viven en DTOs de respuesta y se persisten solo en confirmación/reprogramación.

        return desborde ? EstadoCronograma.ALERTA_DESBORDE : EstadoCronograma.NORMAL;
    }

    /**
     * Offset fijo de las actividades que no están {@code PENDIENTE}. Si una
     * actividad no pendiente no tiene fecha, se fija en {@code t0}, igual que
     * el comportamiento anterior. La fase 2 convierte ese caso en un error de
     * datos explícito (D-08).
     */
    private Map<Long, Long> iniciosFijos(
            List<Actividad> actividades,
            LocalDateTime inicio) {

        Map<Long, Long> fijos = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE
                    || actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            LocalDateTime fecha = actividad.getFechaHoraInicio();
            if (fecha == null) {
                throw new IllegalStateException(
                        "La actividad " + actividad.getIdActividad()
                                + " no pendiente no tiene fecha de inicio.");
            }
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

    // ------------------------------------------------------------------
    // Motor CPM puro
    // ------------------------------------------------------------------

    /**
     * Grafo de actividades no canceladas. Las dependencias hacia actividades
     * fuera del conjunto (por ejemplo, una predecesora cancelada o de otra
     * planificación) se ignoran: la reconexión de sucesoras es responsabilidad
     * de la cancelación (fase 5, D7).
     */
    private record Grafo(
            Map<Long, Actividad> porId,
            Map<Long, List<Long>> sucesoras,
            Map<Long, List<Long>> predecesoras,
            List<Long> orden) {
    }

    private Grafo construirGrafo(List<Actividad> actividades) {
        Map<Long, Actividad> porId = new LinkedHashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            Long id = actividad.getIdActividad();
            if (id == null) {
                throw new CronogramaInviableException(
                        "Hay una actividad sin identificador en el cronograma.");
            }
            if (porId.putIfAbsent(id, actividad) != null) {
                throw new CronogramaInviableException(
                        "La actividad " + id + " está duplicada en el cronograma.");
            }
        }

        Map<Long, List<Long>> sucesoras = new LinkedHashMap<>();
        Map<Long, List<Long>> predecesoras = new LinkedHashMap<>();
        Map<Long, Integer> gradosEntrada = new LinkedHashMap<>();
        for (Long id : porId.keySet()) {
            sucesoras.put(id, new ArrayList<>());
            predecesoras.put(id, new ArrayList<>());
            gradosEntrada.put(id, 0);
        }

        for (Actividad actividad : porId.values()) {
            Set<Actividad> preds = actividad.getPredecesoras();
            if (preds == null) {
                continue;
            }
            Set<Long> idsPredecesoras = new LinkedHashSet<>();
            for (Actividad predecesora : preds) {
                if (predecesora == null || predecesora.getIdActividad() == null) {
                    continue;
                }
                Long idPredecesora = predecesora.getIdActividad();
                if (!porId.containsKey(idPredecesora)
                        || !idsPredecesoras.add(idPredecesora)) {
                    continue;
                }
                predecesoras.get(actividad.getIdActividad()).add(idPredecesora);
                sucesoras.get(idPredecesora).add(actividad.getIdActividad());
                gradosEntrada.merge(actividad.getIdActividad(), 1, Integer::sum);
            }
        }

        Deque<Long> cola = new ArrayDeque<>();
        for (Long id : porId.keySet()) {
            if (gradosEntrada.get(id) == 0) {
                cola.addLast(id);
            }
        }

        List<Long> orden = new ArrayList<>(porId.size());
        while (!cola.isEmpty()) {
            Long id = cola.removeFirst();
            orden.add(id);
            for (Long sucesora : sucesoras.get(id)) {
                if (gradosEntrada.merge(sucesora, -1, Integer::sum) == 0) {
                    cola.addLast(sucesora);
                }
            }
        }

        if (orden.size() != porId.size()) {
            throw new CronogramaInviableException(
                    "El cronograma contiene un ciclo entre actividades.");
        }

        return new Grafo(porId, sucesoras, predecesoras, orden);
    }

    /**
     * Pasada hacia adelante (sección 3.3). Las fijas conservan {@code S_i}; las
     * libres empiezan en {@code max(R, max EF de sus predecesoras)}.
     */
    private void pasadaAdelante(
            Grafo grafo,
            Map<Long, Long> iniciosFijos,
            long releaseTime,
            Map<Long, Long> iniciosTempranos,
            Map<Long, Long> finesTempranos) {

        for (Long id : grafo.orden()) {
            Actividad actividad = grafo.porId().get(id);
            int duracion = duracionValida(actividad);
            Long inicioFijo = iniciosFijos.get(id);

            long inicio;
            if (inicioFijo != null) {
                inicio = inicioFijo;
            } else {
                long finPredecesoras = 0L;
                for (Long predecesora : grafo.predecesoras().get(id)) {
                    finPredecesoras = Math.max(finPredecesoras, finesTempranos.get(predecesora));
                }
                inicio = Math.max(releaseTime, finPredecesoras);
            }

            iniciosTempranos.put(id, inicio);
            finesTempranos.put(id, inicio + duracion);
        }
    }

    /**
     * Pasada hacia atrás doble (secciones 3.4 y 3.5). La pasada de makespan
     * ancla los terminales en {@code M}; la de entrega, en {@code T}. Las fijas
     * se anclan en {@code LS = ES} y {@code LF = EF} en ambas.
     */
    private void pasadaAtras(
            Grafo grafo,
            Map<Long, Long> iniciosFijos,
            Long minutosHastaEntrega,
            Map<Long, Long> iniciosTempranos,
            Map<Long, Long> finesTempranos,
            long makespan,
            Map<Long, Long> iniciosTardiosMakespan,
            Map<Long, Long> finesTardiosMakespan,
            Map<Long, Long> iniciosTardiosEntrega,
            Map<Long, Long> finesTardiosEntrega) {

        List<Long> orden = grafo.orden();
        for (int i = orden.size() - 1; i >= 0; i--) {
            Long id = orden.get(i);
            int duracion = duracionValida(grafo.porId().get(id));
            Long inicioFijo = iniciosFijos.get(id);

            if (inicioFijo != null) {
                iniciosTardiosMakespan.put(id, iniciosTempranos.get(id));
                finesTardiosMakespan.put(id, finesTempranos.get(id));
                if (minutosHastaEntrega != null) {
                    iniciosTardiosEntrega.put(id, iniciosTempranos.get(id));
                    finesTardiosEntrega.put(id, finesTempranos.get(id));
                }
                continue;
            }

            long finTardioMakespan = makespan;
            for (Long sucesora : grafo.sucesoras().get(id)) {
                finTardioMakespan = Math.min(
                        finTardioMakespan, iniciosTardiosMakespan.get(sucesora));
            }
            finesTardiosMakespan.put(id, finTardioMakespan);
            iniciosTardiosMakespan.put(id, finTardioMakespan - duracion);

            if (minutosHastaEntrega != null) {
                long finTardioEntrega = minutosHastaEntrega;
                for (Long sucesora : grafo.sucesoras().get(id)) {
                    finTardioEntrega = Math.min(
                            finTardioEntrega, iniciosTardiosEntrega.get(sucesora));
                }
                finesTardiosEntrega.put(id, finTardioEntrega);
                iniciosTardiosEntrega.put(id, finTardioEntrega - duracion);
            }
        }
    }

    /**
     * Detecta los conflictos estructurales de la sección 3.6. Devuelve, por
     * actividad, los motivos acumulados para exponerlos en el nodo.
     */
    private Map<Long, List<String>> detectarConflictos(
            Grafo grafo,
            Map<Long, Long> iniciosFijos,
            Map<Long, Long> finesTempranos,
            List<ConflictoCpm> conflictos) {

        Map<Long, List<String>> motivos = new LinkedHashMap<>();

        for (Long id : grafo.orden()) {
            Actividad actividad = grafo.porId().get(id);
            Long inicioFijo = iniciosFijos.get(id);
            boolean deberiaEstarFija = actividad.getEstado() == EstadoActividad.EN_CURSO
                    || actividad.getEstado() == EstadoActividad.FINALIZADA;

            if (deberiaEstarFija && inicioFijo == null) {
                registrarConflicto(
                        conflictos,
                        motivos,
                        id,
                        ConflictoCpm.Tipo.DATO_INCONSISTENTE,
                        "La actividad " + id + " está " + actividad.getEstado()
                                + " y no tiene inicio registrado.");
            }

            if (inicioFijo == null) {
                continue;
            }

            if (inicioFijo < 0) {
                registrarConflicto(
                        conflictos,
                        motivos,
                        id,
                        ConflictoCpm.Tipo.FUERA_DE_PROYECTO,
                        "El inicio fijo de la actividad " + id + " (" + inicioFijo
                                + " min) es anterior al inicio del proyecto.");
            }

            for (Long predecesora : grafo.predecesoras().get(id)) {
                long finPredecesora = finesTempranos.get(predecesora);
                if (finPredecesora > inicioFijo) {
                    registrarConflicto(
                            conflictos,
                            motivos,
                            id,
                            ConflictoCpm.Tipo.CONFLICTO_FIJO,
                            "La predecesora " + predecesora + " termina en el minuto "
                                    + finPredecesora + " y la actividad fija " + id
                                    + " comienza en el minuto " + inicioFijo + ".");
                }
            }
        }

        return motivos;
    }

    private void registrarConflicto(
            List<ConflictoCpm> conflictos,
            Map<Long, List<String>> motivos,
            Long idActividad,
            ConflictoCpm.Tipo tipo,
            String detalle) {

        conflictos.add(new ConflictoCpm(idActividad, tipo, detalle));
        motivos.computeIfAbsent(idActividad, id -> new ArrayList<>()).add(detalle);
    }

    /**
     * Marca la cadena que determina el makespan (sección 3.4.1). No se usa
     * {@code critica} para esto: una actividad fija siempre tiene holgura cero
     * aunque no determine la fecha final.
     */
    private Set<Long> marcarRutaCritica(
            Grafo grafo,
            Map<Long, Long> iniciosTempranos,
            Map<Long, Long> finesTempranos,
            long makespan) {

        Set<Long> enRuta = new HashSet<>();
        List<Long> orden = grafo.orden();
        for (int i = orden.size() - 1; i >= 0; i--) {
            Long id = orden.get(i);
            long fin = finesTempranos.get(id);

            boolean pertenece = fin == makespan;
            if (!pertenece) {
                for (Long sucesora : grafo.sucesoras().get(id)) {
                    if (enRuta.contains(sucesora)
                            && Objects.equals(iniciosTempranos.get(sucesora), fin)) {
                        pertenece = true;
                        break;
                    }
                }
            }

            if (pertenece) {
                enRuta.add(id);
            }
        }
        return enRuta;
    }

    private String motivoHolgura(long holguraMakespan, Long holguraEntrega) {
        if (holguraEntrega != null && holguraEntrega < 0) {
            return "Holgura de entrega negativa (" + holguraEntrega + " min).";
        }
        if (holguraMakespan < 0) {
            return "Holgura de makespan negativa (" + holguraMakespan + " min).";
        }
        return null;
    }

    private int duracionValida(Actividad actividad) {
        Integer duracion = actividad.getDuracionMinutos();
        if (duracion == null || duracion < 0) {
            throw new CronogramaInviableException(
                    "La actividad " + actividad.getIdActividad()
                            + " no tiene una duración válida.");
        }
        return duracion;
    }
}
