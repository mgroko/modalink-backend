package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.ActividadReprogramadaResponse;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.dto.ConflictoFijoResponse;
import org.mgroko.backend.proyectos.dto.ReprogramacionProyectoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcula y persiste el cronograma de un proyecto confirmado.
 *
 * <p>Es la pieza común entre el cambio de fecha de entrega y la reprogramación
 * automática por cambio de jornada o margen. La operación es transaccional y
 * atómica por proyecto: si el planificador no encuentra un plan viable, no se
 * persiste ningún cambio parcial.</p>
 *
 * <p>Las actividades {@code PENDIENTE} se limpian y se vuelven a planificar.
 * Las {@code EN_CURSO} y {@code FINALIZADA} conservan su inicio real. Las
 * {@code CANCELADA} no se tocan.</p>
 */
@Service
public class ReprogramarProyectoService {

    private static final Logger LOG =
            LoggerFactory.getLogger(ReprogramarProyectoService.class);

    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final PlanificadorRecursosService planificadorRecursosService;
    private final CalendarioService calendarioService;
    private final CronogramaService cronogramaService;

    public ReprogramarProyectoService(
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            PlanificadorRecursosService planificadorRecursosService,
            CalendarioService calendarioService,
            CronogramaService cronogramaService) {
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.planificadorRecursosService = planificadorRecursosService;
        this.calendarioService = calendarioService;
        this.cronogramaService = cronogramaService;
    }

    /**
     * Reprograma el proyecto completo.
     *
     * @param idProyecto         proyecto confirmado
     * @param motivo             origen de la reprogramación
     * @param nuevaFechaEntrega  nueva entrega, o {@code null} para conservarla
     * @return actividades movidas y conflictos de actividades fijas
     */
    @Transactional
    public ReprogramacionProyectoResponse reprogramar(
            Long idProyecto,
            MotivoReprogramacion motivo,
            LocalDate nuevaFechaEntrega) {
        return reprogramar(idProyecto, motivo, nuevaFechaEntrega, Map.of());
    }

    /**
     * Variante que considera usuarios que todavía no tienen una asignación
     * persistida. Se usa al asignar un miembro: la actividad se mueve antes de
     * insertar la asignación y así el trigger de solape no la rechaza por la
     * fecha anterior.
     */
    @Transactional
    public ReprogramacionProyectoResponse reprogramar(
            Long idProyecto,
            MotivoReprogramacion motivo,
            LocalDate nuevaFechaEntrega,
            Map<Long, List<Long>> usuariosExtraPorActividad) {

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        validarProyectoProgramable(proyecto);

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (nuevaFechaEntrega != null) {
            planificacion.setFechaEntrega(nuevaFechaEntrega);
            planificacionRepository.save(planificacion);
        }

        LocalDateTime fechaInicio = fechaInicio(planificacion);

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(
                        planificacion.getIdPlanificacion());

        Map<Long, LocalDateTime> iniciosAnteriores = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() != EstadoActividad.CANCELADA) {
                iniciosAnteriores.put(
                        actividad.getIdActividad(),
                        actividad.getFechaHoraInicio());
            }
        }

        List<Actividad> pendientes = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.PENDIENTE)
                .toList();

        if (pendientes.isEmpty()) {
            return construirResultado(
                    proyecto,
                    planificacion,
                    motivo,
                    List.of(),
                    conflictosFijos(actividades));
        }

        // Solo las PENDIENTE se replanifican. Las EN_CURSO y FINALIZADA son
        // anclas: se usan para el cálculo, pero no se reescriben.
        Set<Long> movibles = new HashSet<>();
        for (Actividad pendiente : pendientes) {
            movibles.add(pendiente.getIdActividad());
        }

        // Las fechas viejas de las PENDIENTE no deben bloquear el recálculo como
        // si fueran reservas. La transacción las restaura si algo falla.
        for (Actividad actividad : pendientes) {
            actividad.setFechaHoraInicio(null);
        }
        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        PlanificadorRecursosService.Resultado plan;
        try {
            plan = planificadorRecursosService.planificar(
                    planificacion,
                    actividades,
                    fechaInicio,
                    true,
                    usuariosExtraPorActividad);
        } catch (ProyectoNoConfirmableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo recalcular el cronograma del proyecto.",
                    List.of(new BloqueoConfirmacionResponse(
                            "CRONOGRAMA_NO_VIABLE",
                            "No se pudo recalcular el cronograma.",
                            "PROYECTO",
                            idProyecto)));
        }

        List<ActividadReprogramadaResponse> cambios = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }

            LocalDateTime inicio = plan.inicios().get(actividad.getIdActividad());
            if (inicio == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "No se pudo asignar un horario a la actividad '"
                                + actividad.getNombre() + "'.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Actividad sin horario luego de la reprogramación.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }

            // Las actividades ancladas se usaron para calcular el plan, pero
            // no deben persistirse: pueden tener segundos que el planner
            // redondea a minutos y eso rompería márgenes exactos.
            if (!movibles.contains(actividad.getIdActividad())) {
                continue;
            }

            LocalDateTime anterior = iniciosAnteriores.get(actividad.getIdActividad());
            actividad.setFechaHoraInicio(inicio);

            if (!Objects.equals(anterior, inicio)) {
                cambios.add(new ActividadReprogramadaResponse(
                        actividad.getIdActividad(),
                        actividad.getNombre(),
                        actividad.getEstado().name(),
                        anterior,
                        inicio));
            }
        }

        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        List<ConflictoFijoResponse> conflictos = conflictosFijos(actividades);

        LOG.info("Proyecto {} reprogramado por {}. Actividades movidas: {}. Conflictos fijos: {}.",
                idProyecto, motivo, cambios.size(), conflictos.size());

        return construirResultado(
                proyecto,
                planificacion,
                motivo,
                cambios,
                conflictos);
    }

    /**
     * Reprograma solo una actividad y sus sucesoras transitivas
     * {@code PENDIENTE}. Las predecesoras no se mueven.
     */
    @Transactional
    public ReprogramacionProyectoResponse reprogramarActividad(
            Long idProyecto,
            Long idActividad,
            MotivoReprogramacion motivo,
            Map<Long, List<Long>> usuariosExtraPorActividad) {

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));
        validarProyectoProgramable(proyecto);

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        LocalDateTime fechaInicio = fechaInicio(planificacion);

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(
                        planificacion.getIdPlanificacion());
        Map<Long, Actividad> porId = porId(actividades);
        Set<Long> movibles = idsMoviables(actividades, idActividad);
        if (movibles.isEmpty()) {
            throw new ProyectoNoConfirmableException(
                    "ACTIVIDAD_NO_REPROGRAMABLE",
                    "La actividad " + idActividad
                            + " no está pendiente ni tiene sucesoras pendientes.",
                    List.of(new BloqueoConfirmacionResponse(
                            "ACTIVIDAD_NO_REPROGRAMABLE",
                            "La actividad no se puede reprogramar.",
                            "ACTIVIDAD",
                            idActividad)));
        }

        Map<Long, LocalDateTime> iniciosAnteriores = new HashMap<>();
        for (Long id : movibles) {
            Actividad actividad = porId.get(id);
            if (actividad != null) {
                iniciosAnteriores.put(id, actividad.getFechaHoraInicio());
                actividad.setFechaHoraInicio(null);
            }
        }
        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        PlanificadorRecursosService.Resultado plan = planificar(
                planificacion,
                actividades,
                fechaInicio,
                usuariosExtraPorActividad,
                iniciosFijosAdicionales(actividades, movibles));

        List<ActividadReprogramadaResponse> cambios = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }

            LocalDateTime inicio = plan.inicios().get(actividad.getIdActividad());
            if (inicio == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "No se pudo asignar un horario a la actividad '"
                                + actividad.getNombre() + "'.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Actividad sin horario luego de la reprogramación.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }

            // Las actividades ancladas se usaron para calcular el plan, pero
            // no deben persistirse: pueden tener segundos que el planner
            // redondea a minutos y eso rompería márgenes exactos.
            if (!movibles.contains(actividad.getIdActividad())) {
                continue;
            }

            LocalDateTime anterior = iniciosAnteriores.get(actividad.getIdActividad());
            actividad.setFechaHoraInicio(inicio);

            if (!Objects.equals(anterior, inicio)) {
                cambios.add(new ActividadReprogramadaResponse(
                        actividad.getIdActividad(),
                        actividad.getNombre(),
                        actividad.getEstado().name(),
                        anterior,
                        inicio));
            }
        }

        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        List<ConflictoFijoResponse> conflictos = conflictosFijos(actividades);
        LOG.info("Proyecto {} reprogramado por {} (actividad {}). Actividades movidas: {}.",
                idProyecto, motivo, idActividad, cambios.size());

        return construirResultado(
                proyecto, planificacion, motivo, cambios, conflictos);
    }

    /**
     * Simula una asignación sin persistir nada. Devuelve el hueco propuesto
     * para la actividad y el detalle de las actividades que se moverían.
     *
     * <p>No lleva {@code @Transactional}: trabaja sobre copias y, si el
     * planner no encuentra ventana, la excepción de dominio no debe marcar
     * rollback-only la transacción del llamador.</p>
     */
    public SimulacionAsignacion simularAsignacion(
            Long idProyecto,
            Long idActividad,
            List<Long> idsUsuarios) {

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));
        validarProyectoProgramable(proyecto);

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        LocalDateTime fechaInicio = fechaInicio(planificacion);
        List<Actividad> actividades = copiarActividades(
                actividadRepository.findConPredecesorasByPlanificacion(
                        planificacion.getIdPlanificacion()));
        Map<Long, Actividad> porId = porId(actividades);
        Set<Long> movibles = idsMoviables(actividades, idActividad);

        if (movibles.isEmpty()) {
            throw new ProyectoNoConfirmableException(
                    "ACTIVIDAD_NO_REPROGRAMABLE",
                    "La actividad " + idActividad + " no está pendiente.",
                    List.of(new BloqueoConfirmacionResponse(
                            "ACTIVIDAD_NO_REPROGRAMABLE",
                            "La actividad no se puede asignar en su estado actual.",
                            "ACTIVIDAD",
                            idActividad)));
        }

        Map<Long, LocalDateTime> iniciosAnteriores = new HashMap<>();
        for (Long id : movibles) {
            Actividad actividad = porId.get(id);
            if (actividad != null) {
                iniciosAnteriores.put(id, actividad.getFechaHoraInicio());
                actividad.setFechaHoraInicio(null);
            }
        }

        PlanificadorRecursosService.Resultado plan = planificar(
                planificacion,
                actividades,
                fechaInicio,
                Map.of(idActividad, idsUsuarios == null ? List.of() : idsUsuarios),
                iniciosFijosAdicionales(actividades, movibles));

        Actividad objetivo = porId.get(idActividad);
        LocalDateTime inicioPropuesto = plan.inicios().get(idActividad);
        LocalDateTime finPropuesto = objetivo != null && inicioPropuesto != null
                ? inicioPropuesto.plusMinutes(objetivo.getDuracionMinutos())
                : null;

        List<ActividadReprogramadaResponse> cambios = new ArrayList<>();
        for (Long id : movibles) {
            Actividad actividad = porId.get(id);
            if (actividad == null) {
                continue;
            }
            LocalDateTime anterior = iniciosAnteriores.get(id);
            LocalDateTime nuevo = plan.inicios().get(id);
            if (!Objects.equals(anterior, nuevo)) {
                cambios.add(new ActividadReprogramadaResponse(
                        actividad.getIdActividad(),
                        actividad.getNombre(),
                        actividad.getEstado().name(),
                        anterior,
                        nuevo));
            }
        }

        ResultadoDisponibilidad disponibilidad =
                plan.disponibilidad().get(idActividad);
        if (disponibilidad == null && inicioPropuesto != null) {
            disponibilidad = new ResultadoDisponibilidad(
                    true,
                    false,
                    inicioPropuesto,
                    finPropuesto,
                    inicioPropuesto,
                    finPropuesto,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    List.of());
        }

        return new SimulacionAsignacion(
                idActividad,
                inicioPropuesto,
                finPropuesto,
                disponibilidad == null
                        ? null
                        : disponibilidad.conReprogramacionPropuesta(cambios),
                cambios);
    }

    /**
     * Copia las actividades para simular sin tocar entidades gestionadas.
     * El planner solo necesita identidad, estado, duración, inicio,
     * preferencia y predecesoras.
     */
    private List<Actividad> copiarActividades(List<Actividad> originales) {
        Map<Long, Actividad> copias = new LinkedHashMap<>();
        for (Actividad original : originales) {
            if (original.getIdActividad() == null) {
                continue;
            }
            copias.put(
                    original.getIdActividad(),
                    Actividad.builder()
                            .idActividad(original.getIdActividad())
                            .estado(original.getEstado())
                            .nombre(original.getNombre())
                            .descripcion(original.getDescripcion())
                            .duracionMinutos(original.getDuracionMinutos())
                            .fechaHoraInicio(original.getFechaHoraInicio())
                            .horarioPreferenciaInicio(
                                    original.getHorarioPreferenciaInicio())
                            .planificacion(original.getPlanificacion())
                            .ubicacion(original.getUbicacion())
                            .build());
        }

        for (Actividad original : originales) {
            Actividad copia = copias.get(original.getIdActividad());
            if (copia == null || original.getPredecesoras() == null) {
                continue;
            }
            Set<Actividad> predecesoras = new HashSet<>();
            for (Actividad predecesora : original.getPredecesoras()) {
                if (predecesora == null || predecesora.getIdActividad() == null) {
                    continue;
                }
                Actividad copiaPredecesora = copias.get(
                        predecesora.getIdActividad());
                if (copiaPredecesora != null) {
                    predecesoras.add(copiaPredecesora);
                }
            }
            copia.setPredecesoras(predecesoras);
        }

        return new ArrayList<>(copias.values());
    }

    private PlanificadorRecursosService.Resultado planificar(
            Planificacion planificacion,
            List<Actividad> actividades,
            LocalDateTime fechaInicio,
            Map<Long, List<Long>> usuariosExtraPorActividad,
            Map<Long, LocalDateTime> iniciosFijosAdicionales) {

        try {
            return planificadorRecursosService.planificar(
                    planificacion,
                    actividades,
                    fechaInicio,
                    true,
                    usuariosExtraPorActividad == null
                            ? Map.of()
                            : usuariosExtraPorActividad,
                    iniciosFijosAdicionales == null
                            ? Map.of()
                            : iniciosFijosAdicionales);
        } catch (ProyectoNoConfirmableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ProyectoNoConfirmableException(
                    "CRONOGRAMA_NO_VIABLE",
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo recalcular el cronograma del proyecto.",
                    List.of(new BloqueoConfirmacionResponse(
                            "CRONOGRAMA_NO_VIABLE",
                            "No se pudo recalcular el cronograma.",
                            "PROYECTO",
                            planificacion.getProyecto().getIdProyecto())));
        }
    }

    private void validarProyectoProgramable(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado == null || !estado.permiteReprogramacion()) {
            throw new ProyectoNoConfirmableException(
                    "ESTADO_PROYECTO_INVALIDO",
                    "Solo se puede reprogramar un proyecto publicado o confirmado. Estado actual: "
                            + (estado != null
                                    ? estado.getNombre()
                                    : "desconocido")
                            + ".",
                    List.of(new BloqueoConfirmacionResponse(
                            "ESTADO_PROYECTO_INVALIDO",
                            "El proyecto no admite reprogramación.",
                            "PROYECTO",
                            proyecto.getIdProyecto())));
        }
    }

    private LocalDateTime fechaInicio(Planificacion planificacion) {
        Proyecto proyecto = planificacion.getProyecto();
        if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO) {
            LocalDateTime fechaInicio = proyecto.getFechaInicio();
            if (fechaInicio == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "El proyecto confirmado no tiene fecha de inicio.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Proyecto confirmado sin fecha de inicio.",
                                "PROYECTO",
                                proyecto.getIdProyecto())));
            }
            return fechaInicio;
        }

        // Un proyecto publicado aún no tiene fecha de inicio confirmada: se
        // usa el inicio efectivo (hoy) como origen de la planificación.
        return cronogramaService.inicioEfectivo(planificacion);
    }

    private Map<Long, Actividad> porId(List<Actividad> actividades) {
        Map<Long, Actividad> porId = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getIdActividad() != null) {
                porId.put(actividad.getIdActividad(), actividad);
            }
        }
        return porId;
    }

    /**
     * Actividad objetivo + sucesoras transitivas que estén PENDIENTE.
     */
    private Set<Long> idsMoviables(List<Actividad> actividades, Long idObjetivo) {
        Map<Long, List<Long>> sucesoras = new HashMap<>();
        Map<Long, Actividad> porId = porId(actividades);
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA
                    || actividad.getPredecesoras() == null) {
                continue;
            }
            for (Actividad predecesora : actividad.getPredecesoras()) {
                if (predecesora == null || predecesora.getIdActividad() == null) {
                    continue;
                }
                sucesoras
                        .computeIfAbsent(predecesora.getIdActividad(), id -> new ArrayList<>())
                        .add(actividad.getIdActividad());
            }
        }

        Set<Long> movibles = new LinkedHashSet<>();
        Set<Long> visitados = new java.util.HashSet<>();
        Deque<Long> cola = new ArrayDeque<>();
        cola.add(idObjetivo);
        while (!cola.isEmpty()) {
            Long id = cola.removeFirst();
            if (!visitados.add(id)) {
                continue;
            }
            Actividad actividad = porId.get(id);
            if (actividad != null && actividad.getEstado() == EstadoActividad.PENDIENTE) {
                movibles.add(id);
            }
            for (Long sucesora : sucesoras.getOrDefault(id, List.of())) {
                cola.addLast(sucesora);
            }
        }
        return movibles;
    }

    public record SimulacionAsignacion(
            Long idActividad,
            LocalDateTime inicioPropuesto,
            LocalDateTime finPropuesto,
            ResultadoDisponibilidad disponibilidad,
            List<ActividadReprogramadaResponse> actividadesAfectadas) {

        public SimulacionAsignacion {
            actividadesAfectadas = actividadesAfectadas == null
                    ? List.of()
                    : List.copyOf(actividadesAfectadas);
        }
    }

    private ReprogramacionProyectoResponse construirResultado(
            Proyecto proyecto,
            Planificacion planificacion,
            MotivoReprogramacion motivo,
            List<ActividadReprogramadaResponse> cambios,
            List<ConflictoFijoResponse> conflictos) {

        return new ReprogramacionProyectoResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                motivo != null ? motivo.name() : null,
                planificacion.getFechaEntrega(),
                cambios,
                conflictos);
    }

    /**
     * Detecta actividades fijas que ya no entran en la nueva disponibilidad de
     * alguno de sus asignados. No las mueve: solo las reporta.
     */
    private List<ConflictoFijoResponse> conflictosFijos(List<Actividad> actividades) {
        List<Actividad> fijas = actividades.stream()
                .filter(actividad -> actividad.getEstado() == EstadoActividad.EN_CURSO
                        || actividad.getEstado() == EstadoActividad.FINALIZADA)
                .filter(actividad -> actividad.getFechaHoraInicio() != null)
                .toList();

        if (fijas.isEmpty()) {
            return List.of();
        }

        List<Long> idsFijas = fijas.stream()
                .map(Actividad::getIdActividad)
                .toList();

        Map<Long, List<AsignacionActividad>> asignacionesPorActividad = new HashMap<>();
        for (AsignacionActividad asignacion
                : asignacionActividadRepository.findActivasByActividadIds(idsFijas)) {
            if (asignacion.getActividad() == null
                    || asignacion.getActividad().getIdActividad() == null) {
                continue;
            }
            asignacionesPorActividad
                    .computeIfAbsent(
                            asignacion.getActividad().getIdActividad(),
                            id -> new ArrayList<>())
                    .add(asignacion);
        }

        List<ConflictoFijoResponse> conflictos = new ArrayList<>();
        for (Actividad fija : fijas) {
            List<Long> usuarios = usuariosDe(
                    asignacionesPorActividad.getOrDefault(
                            fija.getIdActividad(), List.of()));
            if (usuarios.isEmpty()) {
                continue;
            }

            try {
                boolean disponible = calendarioService.ventanaDisponiblePara(
                        usuarios,
                        fija.getFechaHoraInicio(),
                        fija.getFechaHoraFin(),
                        fija.getIdActividad());
                if (!disponible) {
                    conflictos.add(new ConflictoFijoResponse(
                            fija.getIdActividad(),
                            fija.getNombre(),
                            usuarios,
                            "La actividad " + fija.getEstado().name().toLowerCase()
                                    + " quedó fuera de la nueva disponibilidad y no se movió."));
                }
            } catch (RuntimeException ex) {
                conflictos.add(new ConflictoFijoResponse(
                        fija.getIdActividad(),
                        fija.getNombre(),
                        usuarios,
                        "No se pudo verificar la disponibilidad de la actividad fija: "
                                + ex.getMessage()));
            }
        }

        return conflictos;
    }

    /**
     * Ancla temporalmente las {@code PENDIENTE} que no forman parte de la
     * reprogramación acotada. El anclaje es solo para esta operación: no se
     * persiste ni cambia la regla global de movilidad.
     */
    private Map<Long, LocalDateTime> iniciosFijosAdicionales(
            List<Actividad> actividades,
            Set<Long> movibles) {

        Map<Long, LocalDateTime> fijos = new HashMap<>();
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE
                    && !movibles.contains(actividad.getIdActividad())
                    && actividad.getFechaHoraInicio() != null) {
                fijos.put(
                        actividad.getIdActividad(),
                        actividad.getFechaHoraInicio());
            }
        }
        return fijos;
    }

    private List<Long> usuariosDe(Collection<AsignacionActividad> asignaciones) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            if (asignacion.getMiembro() == null
                    || asignacion.getMiembro().getPerfil() == null
                    || asignacion.getMiembro().getPerfil().getUsuario() == null) {
                continue;
            }
            ids.add(asignacion.getMiembro().getPerfil().getUsuario().getIdUsuario());
        }
        return List.copyOf(ids);
    }
}
