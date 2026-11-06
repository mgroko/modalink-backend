package org.mgroko.backend.proyectos.servicio;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.ConflictoCpm;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;
import org.mgroko.backend.calendario.servicio.MargenActividad;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.springframework.stereotype.Service;

/**
 * Planificador greedy de recursos para UC-60.
 *
 * <p>Recorre el orden topológico del CPM y coloca cada actividad en el primer
 * hueco común de sus asignados. El CPM se calcula una sola vez; la ocupación
 * de las actividades que se van colocando se mantiene en memoria para que la
 * evaluación de las siguientes actividades las vea sin depender de un flush
 * intermedio.</p>
 *
 * <p>El cálculo usa el mismo {@code releaseTime} que el cronograma de lectura:
 * las actividades libres nunca empiezan antes de la hora actual, mientras que
 * las fijas conservan su inicio real aunque esté en el pasado.</p>
 */
@Service
public class PlanificadorRecursosService {

    private static final java.time.LocalTime HORA_FIN_ENTREGA =
            java.time.LocalTime.of(23, 59);

    private final CronogramaService cronogramaService;
    private final CalendarioService calendarioService;
    private final AsignacionActividadRepository asignacionActividadRepository;

    public PlanificadorRecursosService(
            CronogramaService cronogramaService,
            CalendarioService calendarioService,
            AsignacionActividadRepository asignacionActividadRepository) {
        this.cronogramaService = cronogramaService;
        this.calendarioService = calendarioService;
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    public Resultado planificar(
            Planificacion planificacion,
            List<Actividad> actividades,
            LocalDateTime fechaInicio) {
        return planificar(
                planificacion, actividades, fechaInicio, false);
    }

    /**
     * Variante que permite agendar actividades sin integrantes activos.
     *
     * <p>Se usa en proyectos confirmados que ya pueden contener actividades
     * nuevas sin asignar. Las actividades fijas sin asignación conservan su
     * inicio; las libres se colocan en su inicio temprano. En ambos casos no
     * reservan bloques para ningún usuario.</p>
     */
    public Resultado planificar(
            Planificacion planificacion,
            List<Actividad> actividades,
            LocalDateTime fechaInicio,
            boolean permitirSinAsignaciones) {
        return planificar(
                planificacion,
                actividades,
                fechaInicio,
                permitirSinAsignaciones,
                Map.of());
    }

    /**
     * Variante que agrega usuarios virtuales a una actividad.
     *
     * <p>Se usa antes de insertar una asignación: el planificador considera al
     * candidato como si ya estuviera asignado, mueve la actividad a un hueco
     * común y recién después se persiste la asignación. Así la disponibilidad
     * real gobierna la reprogramación y el trigger de solape no rechaza la
     * asignación por la fecha vieja.</p>
     */
    public Resultado planificar(
            Planificacion planificacion,
            List<Actividad> actividades,
            LocalDateTime fechaInicio,
            boolean permitirSinAsignaciones,
            Map<Long, List<Long>> usuariosExtraPorActividad) {

        return planificar(
                planificacion,
                actividades,
                fechaInicio,
                permitirSinAsignaciones,
                usuariosExtraPorActividad,
                Map.of());
    }

    /**
     * Variante que permite anclar actividades adicionales durante una
     * reprogramación acotada.
     *
     * <p>La regla global solo considera inmóviles a {@code EN_CURSO} y
     * {@code FINALIZADA}. Una reprogramación de una actividad puntual puede
     * fijar temporalmente el resto de las {@code PENDIENTE} para no moverlas
     * sin necesidad; ese anclaje es de la operación y no se persiste.</p>
     */
    public Resultado planificar(
            Planificacion planificacion,
            List<Actividad> actividades,
            LocalDateTime fechaInicio,
            boolean permitirSinAsignaciones,
            Map<Long, List<Long>> usuariosExtraPorActividad,
            Map<Long, LocalDateTime> iniciosFijosAdicionales) {

        Map<Long, List<Long>> extras = usuariosExtraPorActividad == null
                ? Map.of()
                : usuariosExtraPorActividad;
        Map<Long, LocalDateTime> fijosAdicionales = iniciosFijosAdicionales == null
                ? Map.of()
                : iniciosFijosAdicionales;
        LocalDate fechaEntrega = planificacion.getFechaEntrega();
        if (fechaEntrega == null) {
            throw new ProyectoNoConfirmableException(
                    "FECHA_ENTREGA_INVALIDA",
                    "La planificación no tiene fecha de entrega.");
        }

        LocalDateTime finEntrega = fechaEntrega.atTime(HORA_FIN_ENTREGA);
        if (!finEntrega.isAfter(fechaInicio)) {
            throw new ProyectoNoConfirmableException(
                    "FECHA_ENTREGA_INVALIDA",
                    "La fecha de entrega debe ser posterior a la fecha de inicio.");
        }

        Map<Long, Actividad> actividadesPorId = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }
            actividadesPorId.put(actividad.getIdActividad(), actividad);
            ids.add(actividad.getIdActividad());
        }

        // Las fechas persistidas de estas mismas actividades son solo una
        // proyección previa del cronograma. El planificador las recoloca y las
        // modela en memoria, así que no deben contarse otra vez como bloqueos
        // externos del calendario.
        Set<Long> idsActividadesPropias = actividadesPorId.keySet();

        Map<Long, Long> iniciosFijos = construirIniciosFijos(
                actividades,
                fechaInicio,
                fijosAdicionales);

        long minutosHastaEntrega = Duration.between(fechaInicio, finEntrega).toMinutes();
        LocalDateTime ahora = cronogramaService.ahora();
        long releaseTime = cronogramaService.releaseTime(fechaInicio, ahora);
        CalculoCpm calculo;
        try {
            calculo = cronogramaService.calcular(
                    actividades,
                    iniciosFijos,
                    minutosHastaEntrega,
                    releaseTime);
        } catch (RuntimeException ex) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo calcular el cronograma del proyecto.");
        }

        if (!Boolean.TRUE.equals(calculo.viable())) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    "El cronograma no es viable: revisá la ruta crítica, los conflictos y la fecha de entrega.",
                    bloqueosDeCalculo(calculo));
        }

        Map<Long, List<AsignacionActividad>> asignacionesPorActividad =
                cargarAsignaciones(ids);
        List<Long> orden = ordenPlanificacion(calculo, actividadesPorId);
        Map<Long, LocalDateTime> inicios = new LinkedHashMap<>();
        Map<Long, ResultadoDisponibilidad> disponibilidad = new LinkedHashMap<>();
        Map<Long, List<Intervalo>> bloqueosExtra = new HashMap<>();

        // Las actividades fijas reservan su intervalo antes de colocar las
        // libres. Así el orden topológico no permite que una actividad libre
        // se superponga con una fija que aparece más tarde en el grafo.
        for (Long idActividad : orden) {
            NodoCpm nodo = calculo.nodoDe(idActividad);
            if (!nodo.fijo()) {
                continue;
            }
            Actividad actividad = actividadesPorId.get(idActividad);
            List<AsignacionActividad> asignaciones = asignacionesPorActividad
                    .getOrDefault(idActividad, List.of());
            List<Long> usuarios = usuariosDe(
                    asignaciones, extras.getOrDefault(idActividad, List.of()));
            LocalDateTime inicio = inicioExacto(
                    actividad, idActividad, nodo, fechaInicio, fijosAdicionales);
            if (usuarios.isEmpty()) {
                if (!permitirSinAsignaciones) {
                    throw new ProyectoNoConfirmableException(
                            "ACTIVIDADES_SIN_ASIGNACION",
                            "La actividad '" + actividad.getNombre()
                                    + "' no tiene integrantes activos asignados.",
                            List.of(new BloqueoConfirmacionResponse(
                                    "ACTIVIDADES_SIN_ASIGNACION",
                                    "La actividad no tiene integrantes activos asignados.",
                                    "ACTIVIDAD",
                                    idActividad)));
                }
                inicios.put(idActividad, inicio);
                continue;
            }
            inicios.put(idActividad, inicio);
            agregarBloqueoValidandoSolape(
                    bloqueosExtra, usuarios, inicio, actividad);
        }

        for (Long idActividad : orden) {
            Actividad actividad = actividadesPorId.get(idActividad);
            if (actividad == null) {
                continue;
            }
            NodoCpm nodo = calculo.nodoDe(idActividad);
            if (nodo.fijo()) {
                continue;
            }

            List<AsignacionActividad> asignaciones = asignacionesPorActividad
                    .getOrDefault(idActividad, List.of());
            List<Long> usuarios = usuariosDe(
                    asignaciones, extras.getOrDefault(idActividad, List.of()));

            LocalDateTime inicioMinimo = fechaInicio.plusMinutes(nodo.inicioTemprano());
            if (inicioMinimo.isBefore(ahora)) {
                inicioMinimo = ahora;
            }
            for (Actividad predecesora : actividad.getPredecesoras()) {
                if (predecesora == null
                        || predecesora.getEstado() == EstadoActividad.CANCELADA) {
                    continue;
                }
                LocalDateTime inicioPredecesora = inicios.get(predecesora.getIdActividad());
                LocalDateTime finPredecesora = inicioPredecesora != null
                        ? inicioPredecesora.plusMinutes(predecesora.getDuracionMinutos())
                        : predecesora.getFechaHoraFin();
                if (finPredecesora == null) {
                    throw new ProyectoNoConfirmableException(
                            "DATOS_INCONSISTENTES",
                            "La predecesora '" + predecesora.getNombre()
                                    + "' no tiene un horario calculable.",
                            List.of(new BloqueoConfirmacionResponse(
                                    "DATOS_INCONSISTENTES",
                                    "La predecesora no tiene un horario calculable.",
                                    "ACTIVIDAD",
                                    predecesora.getIdActividad())));
                }
                if (finPredecesora.isAfter(inicioMinimo)) {
                    inicioMinimo = finPredecesora;
                }
            }

            long ultimoInicio = nodo.inicioTardioEntrega() != null
                    ? nodo.inicioTardioEntrega()
                    : nodo.inicioTardioMakespan();
            LocalDateTime inicioMaximo = fechaInicio.plusMinutes(ultimoInicio);
            if (inicioMaximo.isBefore(inicioMinimo)) {
                throw new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "La actividad '" + actividad.getNombre()
                                + "' no tiene una ventana de inicio compatible con sus predecesoras y la entrega.",
                        List.of(new BloqueoConfirmacionResponse(
                                "SIN_DISPONIBILIDAD",
                                "La ventana de inicio quedó vacía.",
                                "ACTIVIDAD",
                                idActividad)));
            }

            if (usuarios.isEmpty()) {
                if (!permitirSinAsignaciones) {
                    throw new ProyectoNoConfirmableException(
                            "ACTIVIDADES_SIN_ASIGNACION",
                            "La actividad '" + actividad.getNombre()
                                    + "' no tiene integrantes activos asignados.",
                            List.of(new BloqueoConfirmacionResponse(
                                    "ACTIVIDADES_SIN_ASIGNACION",
                                    "La actividad no tiene integrantes activos asignados.",
                                    "ACTIVIDAD",
                                    idActividad)));
                }
                inicios.put(idActividad, inicioMinimo);
                continue;
            }

            ResultadoDisponibilidad resultado =
                    calendarioService.verificarDisponibilidadMiembrosEnRango(
                            usuarios,
                            actividad,
                            inicioMinimo,
                            inicioMaximo,
                            bloqueosExtra,
                            idsActividadesPropias);
            disponibilidad.put(idActividad, resultado);

            if (!resultado.disponible() || resultado.inicioSeleccionado() == null) {
                String motivo = resultado.motivo() != null
                        ? resultado.motivo()
                        : "SIN_HUECO";
                throw new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "La actividad '" + actividad.getNombre()
                                + "' no tiene disponibilidad común para todos sus integrantes. Motivo: "
                                + motivo + ".",
                        List.of(new BloqueoConfirmacionResponse(
                                "SIN_DISPONIBILIDAD",
                                "No hay hueco común. Motivo: " + motivo + ".",
                                "ACTIVIDAD",
                                idActividad)));
            }

            LocalDateTime inicio = resultado.inicioSeleccionado();
            inicios.put(idActividad, inicio);
            agregarBloqueo(bloqueosExtra, usuarios, inicio, actividad);
        }

        for (Map.Entry<Long, LocalDateTime> entrada : inicios.entrySet()) {
            Actividad actividad = actividadesPorId.get(entrada.getKey());
            if (actividad == null) {
                continue;
            }

            NodoCpm nodo = calculo.nodoDe(entrada.getKey());
            if (nodo != null
                    && !nodo.fijo()
                    && entrada.getValue().isBefore(ahora)) {
                throw new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "La actividad '" + actividad.getNombre()
                                + "' quedaría programada en el pasado.",
                        List.of(new BloqueoConfirmacionResponse(
                                "SIN_DISPONIBILIDAD",
                                "El inicio planificado es anterior a la hora actual.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }

            LocalDateTime fin = entrada.getValue()
                    .plusMinutes(actividad.getDuracionMinutos());
            if (fin.isAfter(finEntrega)) {
                throw new ProyectoNoConfirmableException(
                        "CRONOGRAMA_NO_VIABLE",
                        "La actividad '" + actividad.getNombre()
                                + "' termina después de la fecha de entrega.",
                        List.of(new BloqueoConfirmacionResponse(
                                "CRONOGRAMA_NO_VIABLE",
                                "El fin planificado supera la fecha de entrega.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }
        }

        return new Resultado(inicios, disponibilidad, calculo);
    }

    private List<Long> ordenPlanificacion(
            CalculoCpm calculo,
            Map<Long, Actividad> actividadesPorId) {

        Map<Long, Integer> grados = new HashMap<>();
        Map<Long, List<Long>> sucesoras = new HashMap<>();
        for (Long id : calculo.nodos().keySet()) {
            grados.put(id, 0);
            sucesoras.put(id, new ArrayList<>());
        }

        for (Long id : calculo.nodos().keySet()) {
            Actividad actividad = actividadesPorId.get(id);
            if (actividad == null || actividad.getPredecesoras() == null) {
                continue;
            }
            for (Actividad predecesora : actividad.getPredecesoras()) {
                if (predecesora == null
                        || predecesora.getEstado() == EstadoActividad.CANCELADA
                        || !calculo.nodos().containsKey(predecesora.getIdActividad())) {
                    continue;
                }
                sucesoras.get(predecesora.getIdActividad()).add(id);
                grados.merge(id, 1, Integer::sum);
            }
        }

        Comparator<Long> prioridad = Comparator
                .comparingLong((Long id) -> holgura(calculo.nodoDe(id)))
                .thenComparingLong(id -> calculo.nodoDe(id).inicioTemprano())
                .thenComparingLong(id -> id);

        PriorityQueue<Long> listas = new PriorityQueue<>(prioridad);
        for (Map.Entry<Long, Integer> entrada : grados.entrySet()) {
            if (entrada.getValue() == 0) {
                listas.add(entrada.getKey());
            }
        }

        List<Long> orden = new ArrayList<>(calculo.nodos().size());
        while (!listas.isEmpty()) {
            Long id = listas.poll();
            orden.add(id);
            for (Long sucesora : sucesoras.get(id)) {
                if (grados.merge(sucesora, -1, Integer::sum) == 0) {
                    listas.add(sucesora);
                }
            }
        }

        if (orden.size() != calculo.nodos().size()) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    "El cronograma contiene un ciclo entre actividades.");
        }
        return orden;
    }

    private long holgura(NodoCpm nodo) {
        return nodo.holguraEntrega() != null
                ? nodo.holguraEntrega()
                : Long.MAX_VALUE;
    }

    /**
     * Inicio exacto de una actividad anclada. El CPM trabaja en minutos
     * enteros, pero persistir o comparar una ancla con esa precisión perdería
     * los segundos y podría adelantar una actividad respecto de su predecesora.
     */
    private LocalDateTime inicioExacto(
            Actividad actividad,
            Long idActividad,
            NodoCpm nodo,
            LocalDateTime fechaInicio,
            Map<Long, LocalDateTime> fijosAdicionales) {

        if (actividad.getEstado() == EstadoActividad.EN_CURSO
                || actividad.getEstado() == EstadoActividad.FINALIZADA) {
            return actividad.getFechaHoraInicio();
        }
        LocalDateTime inicioAdicional = fijosAdicionales.get(idActividad);
        return inicioAdicional != null
                ? inicioAdicional
                : fechaInicio.plusMinutes(nodo.inicioFijo());
    }

    private Map<Long, Long> construirIniciosFijos(
            List<Actividad> actividades,
            LocalDateTime fechaInicio,
            Map<Long, LocalDateTime> iniciosFijosAdicionales) {

        Map<Long, Long> fijos = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }

            boolean inmóvil = actividad.getEstado() == EstadoActividad.EN_CURSO
                    || actividad.getEstado() == EstadoActividad.FINALIZADA;
            LocalDateTime inicioAdicional = iniciosFijosAdicionales.get(
                    actividad.getIdActividad());
            boolean fija = inmóvil || inicioAdicional != null;
            if (!fija) {
                continue;
            }

            LocalDateTime fecha = inicioAdicional != null
                    ? inicioAdicional
                    : actividad.getFechaHoraInicio();
            if (fecha == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "La actividad '" + actividad.getNombre()
                                + "' está en un estado fijo pero no tiene fecha de inicio.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Actividad fija sin fecha de inicio.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }
            fijos.put(
                    actividad.getIdActividad(),
                    Duration.between(fechaInicio, fecha).toMinutes());
        }
        return fijos;
    }

    private Map<Long, List<AsignacionActividad>> cargarAsignaciones(List<Long> ids) {
        Map<Long, List<AsignacionActividad>> resultado = new HashMap<>();
        if (ids.isEmpty()) {
            return resultado;
        }
        for (AsignacionActividad asignacion
                : asignacionActividadRepository.findActivasByActividadIds(ids)) {
            if (asignacion.getActividad() == null
                    || asignacion.getActividad().getIdActividad() == null) {
                continue;
            }
            resultado.computeIfAbsent(
                    asignacion.getActividad().getIdActividad(),
                    id -> new ArrayList<>()).add(asignacion);
        }
        return resultado;
    }

    private List<Long> usuariosDe(Collection<AsignacionActividad> asignaciones) {
        return usuariosDe(asignaciones, List.of());
    }

    private List<Long> usuariosDe(
            Collection<AsignacionActividad> asignaciones,
            Collection<Long> extras) {

        Set<Long> ids = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getMiembro() == null
                    || asignacion.getMiembro().getPerfil() == null
                    || asignacion.getMiembro().getPerfil().getUsuario() == null) {
                continue;
            }
            ids.add(asignacion.getMiembro().getPerfil().getUsuario().getIdUsuario());
        }
        if (extras != null) {
            extras.stream()
                    .filter(Objects::nonNull)
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private void agregarBloqueo(
            Map<Long, List<Intervalo>> bloqueosExtra,
            List<Long> usuarios,
            LocalDateTime inicio,
            Actividad actividad) {

        Intervalo intervalo = new Intervalo(
                inicio,
                inicio.plusMinutes(actividad.getDuracionMinutos()));
        for (Long idUsuario : usuarios) {
            bloqueosExtra.computeIfAbsent(idUsuario, id -> new ArrayList<>())
                    .add(intervalo);
        }
    }

    private void agregarBloqueoValidandoSolape(
            Map<Long, List<Intervalo>> bloqueosExtra,
            List<Long> usuarios,
            LocalDateTime inicio,
            Actividad actividad) {

        Intervalo intervalo = new Intervalo(
                inicio,
                inicio.plusMinutes(actividad.getDuracionMinutos()));
        for (Long idUsuario : usuarios) {
            List<Intervalo> existentes = bloqueosExtra
                    .computeIfAbsent(idUsuario, id -> new ArrayList<>());
            int margen = calendarioService.margenEfectivoDe(idUsuario);
            Intervalo intervaloConMargen = expandirConMargen(intervalo, margen);
            boolean solapa = existentes.stream()
                    .map(existente -> expandirConMargen(existente, margen))
                    .anyMatch(intervaloConMargen::solapa);
            if (solapa) {
                throw new ProyectoNoConfirmableException(
                        "CRONOGRAMA_NO_VIABLE",
                        "Las actividades fijas del proyecto se superponen para un mismo integrante.",
                        List.of(new BloqueoConfirmacionResponse(
                                "CRONOGRAMA_NO_VIABLE",
                                "Solapamiento entre actividades fijas.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }
            // Se guarda el intervalo crudo: la evaluación de disponibilidad
            // posterior lo expande con el margen del usuario una sola vez.
            existentes.add(intervalo);
        }
    }

    private Intervalo expandirConMargen(Intervalo intervalo, int margen) {
        return new Intervalo(
                intervalo.inicio().minusMinutes(MargenActividad.izquierdo(margen)),
                intervalo.fin().plusMinutes(MargenActividad.derecho(margen)));
    }

    private List<BloqueoConfirmacionResponse> bloqueosDeCalculo(CalculoCpm calculo) {
        List<BloqueoConfirmacionResponse> bloqueos = new ArrayList<>();
        for (ConflictoCpm conflicto : calculo.conflictos()) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "CRONOGRAMA_NO_VIABLE",
                    conflicto.detalle(),
                    "ACTIVIDAD",
                    conflicto.idActividad()));
        }
        if (bloqueos.isEmpty()) {
            bloqueos.add(new BloqueoConfirmacionResponse(
                    "CRONOGRAMA_NO_VIABLE",
                    "El cronograma no es viable.",
                    "CRONOGRAMA",
                    null));
        }
        return bloqueos;
    }

    public record Resultado(
            Map<Long, LocalDateTime> inicios,
            Map<Long, ResultadoDisponibilidad> disponibilidad,
            CalculoCpm calculo) {

        public Resultado {
            inicios = inicios == null ? Map.of() : Map.copyOf(inicios);
            disponibilidad = disponibilidad == null ? Map.of() : Map.copyOf(disponibilidad);
        }
    }
}
