package org.mgroko.backend.actividades.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.actividades.evento.ActividadCanceladaEvent;
import org.mgroko.backend.actividades.evento.ActividadModificadaEvent;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.CicloActividadException;
import org.mgroko.backend.actividades.exception.DuracionInvalidaException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteModificarActividadesException;
import org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException;
import org.mgroko.backend.actividades.exception.PredecesoraInvalidaException;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.RequerimientoSnapshotService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModificarActividadService {

    public static final String PERMISO_MODIFICAR_ACTIVIDAD = "MODIFICAR_ACTIVIDAD";

    private final ActividadRepository actividadRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final UbicacionService ubicacionService;
    private final RequerimientoActividadService requerimientoActividadService;
    private final RequerimientoProteccionService requerimientoProteccionService;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final CronogramaService cronogramaService;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final RequerimientoSnapshotService requerimientoSnapshotService;
    private final ApplicationEventPublisher eventPublisher;
    private final ActividadOrigenService actividadOrigenService;
    private final Clock reloj;

    public ModificarActividadService(
            ActividadRepository actividadRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            UbicacionService ubicacionService,
            RequerimientoActividadService requerimientoActividadService,
            RequerimientoProteccionService requerimientoProteccionService,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            CronogramaService cronogramaService,
            AsignacionActividadRepository asignacionActividadRepository,
            ReprogramarProyectoService reprogramarProyectoService,
            RequerimientoSnapshotService requerimientoSnapshotService,
            ApplicationEventPublisher eventPublisher,
            ActividadOrigenService actividadOrigenService,
            Clock reloj) {
        this.actividadRepository = actividadRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.ubicacionService = ubicacionService;
        this.requerimientoActividadService = requerimientoActividadService;
        this.requerimientoProteccionService = requerimientoProteccionService;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.cronogramaService = cronogramaService;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.requerimientoSnapshotService = requerimientoSnapshotService;
        this.eventPublisher = eventPublisher;
        this.actividadOrigenService = actividadOrigenService;
        this.reloj = reloj;
    }

    @Transactional
    public ActividadResponse modificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            ModificarActividadRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));
        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_MODIFICAR_ACTIVIDAD);
        validarEstadoProyecto(proyecto);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        ActividadEstadoValidator.validar(actividad.getEstado(), request.estado());
        EstadoActividad estadoAnterior = actividad.getEstado();

        int duracionMinutos = calcularDuracionMinutos(request);
        validarEstructuraConfirmada(proyecto, actividad, request, duracionMinutos);

        String nombre = request.nombre().trim();
        if (actividadRepository.existeNombreEnPlanificacionExcluyendo(
                planificacion.getIdPlanificacion(), nombre, idActividad)) {
            throw new NombreActividadDuplicadoException(nombre);
        }

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());

        actualizarDatos(actividad, request, nombre, duracionMinutos);
        actualizarUbicacion(actividad, request);
        actualizarPredecesoras(actividad, request.predecesoras(), planificacion);
        ActividadEstadoValidator.validarPredecesorasFinalizadas(
                actividad, request.estado());
        validarSinCiclos(actividad, actividades);
        actualizarRequerimientos(actividad, request.requerimientos());

        Actividad guardada = guardarActividad(actividad);

        if (estadoAnterior != EstadoActividad.CANCELADA
                && guardada.getEstado() == EstadoActividad.CANCELADA) {
            List<Actividad> paraReconectar = actividadRepository
                    .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
            ActividadDependencias.reconectarSucesoras(
                    actividadRepository, guardada, paraReconectar);

            if (proyecto.getEstado() != null
                    && proyecto.getEstado().permiteReprogramacion()
                    && planificacion.getFechaEntrega() != null) {
                reprogramarProyectoService.reprogramar(
                        idProyecto,
                        MotivoReprogramacion.CANCELACION_ACTIVIDAD,
                        null);
            }

            eventPublisher.publishEvent(new ActividadCanceladaEvent(
                    guardada.getIdActividad(),
                    proyecto.getIdProyecto(),
                    guardada.getNombre(),
                    proyecto.getNombre()));
        }

        // Sin fecha de entrega no se calcula CPM: la actividad queda flotante.
        var calculo = planificacion.getFechaEntrega() == null
                ? null
                : cronogramaService.calcularProyeccion(
                        planificacion,
                        actividadRepository.findConPredecesorasByPlanificacion(
                                planificacion.getIdPlanificacion()));
        EstadoCronograma estadoCronograma = calculo == null
                ? EstadoCronograma.SIN_FECHA_ENTREGA
                : cronogramaService.estadoDe(calculo);

        List<RequerimientoActividadResponse> requerimientos =
                requerimientoActividadService.listarPorActividad(idActividad).stream()
                        .map(requerimiento -> requerimiento.conProtegido(
                                requerimientoProteccionService.estaProtegido(
                                        requerimiento.idRequerimiento())))
                        .toList();

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findActivasByActividadId(idActividad);

        publicarActividadModificada(guardada, proyecto, asignaciones);

        return ActividadMapper.toResponse(
                guardada,
                requerimientos,
                asignaciones.stream()
                        .map(ActividadMapper::toProfesionalAsignado)
                        .toList(),
                estadoCronograma,
                calculo,
                calculo == null ? null : cronogramaService.inicioEfectivo(planificacion),
                calculo == null || cronogramaService.esTentativa(planificacion),
                actividadOrigenService.calcular(guardada));
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoPermiteModificarActividadesException(estado.name());
        }
    }

    /**
     * En un proyecto confirmado no se pueden cambiar nombre, duración ni
     * predecesoras. Los requerimientos se validan individualmente en
     * {@link #actualizarRequerimientos}: los que tienen postulaciones
     * pendientes/aceptadas o asignaciones quedan inmutables; el resto se puede
     * mantener, modificar, agregar o quitar.
     */
    private void validarEstructuraConfirmada(
            Proyecto proyecto,
            Actividad actividad,
            ModificarActividadRequest request,
            int duracionMinutos) {

        if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            return;
        }

        boolean cambioNombre = !Objects.equals(
                actividad.getNombre(), request.nombre().trim());
        boolean cambioDuracion = !Integer.valueOf(duracionMinutos)
                .equals(actividad.getDuracionMinutos());

        Set<Long> predecesorasActuales = actividad.getPredecesoras().stream()
                .filter(predecesora -> predecesora != null
                        && predecesora.getIdActividad() != null)
                .map(Actividad::getIdActividad)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> predecesorasNuevas = request.predecesoras() == null
                ? Set.of()
                : new HashSet<>(request.predecesoras());

        if (cambioNombre
                || cambioDuracion
                || !predecesorasActuales.equals(predecesorasNuevas)) {
            throw new EstadoProyectoNoPermiteModificarActividadesException(
                    EstadoProyecto.CONFIRMADO.name());
        }
    }

    private int calcularDuracionMinutos(ModificarActividadRequest request) {
        if (request.duracion() == null || request.unidadDuracion() == null) {
            throw new DuracionInvalidaException(
                    "La duración y su unidad son obligatorias. Usá HORAS o MINUTOS.");
        }

        long minutos;
        try {
            minutos = request.unidadDuracion().aMinutos(request.duracion());
        } catch (ArithmeticException ex) {
            throw new DuracionInvalidaException("La duración indicada es demasiado grande.");
        }

        if (minutos <= 0 || minutos > Integer.MAX_VALUE) {
            throw new DuracionInvalidaException(
                    "La duración debe ser mayor que cero y expresarse en un rango válido.");
        }

        return (int) minutos;
    }

    private void actualizarDatos(
            Actividad actividad,
            ModificarActividadRequest request,
            String nombre,
            int duracionMinutos) {
        actividad.setNombre(nombre);
        actividad.setDescripcion(
                request.descripcion() != null ? request.descripcion().trim() : null);
        actividad.setDuracionMinutos(duracionMinutos);
        actividad.setHorarioPreferenciaInicio(request.horarioPreferenciaInicio());
        actividad.setEstado(request.estado());
    }

    private void actualizarUbicacion(Actividad actividad, ModificarActividadRequest request) {
        if (request.ubicacion() == null || request.ubicacion().localidadId() == null
                || request.ubicacion().localidadId().isBlank()) {
            actividad.setUbicacion(null);
            return;
        }

        Ubicacion ubicacion = ubicacionService.obtenerOCrear(
                request.ubicacion().localidadId(),
                request.ubicacion().provinciaId());
        actividad.setUbicacion(ubicacion);
    }

    private void actualizarPredecesoras(
            Actividad actividad,
            List<Long> idsPredecesoras,
            Planificacion planificacion) {

        if (idsPredecesoras == null) {
            throw new PredecesoraInvalidaException(
                    "La lista de predecesoras es obligatoria.");
        }

        actividad.setPredecesoras(resolverPredecesoras(
                idsPredecesoras,
                planificacion.getIdPlanificacion(),
                actividad.getIdActividad()));
    }

    private Set<Actividad> resolverPredecesoras(
            List<Long> idsPredecesoras,
            Long idPlanificacion,
            Long idActividad) {

        Set<Actividad> resultado = new LinkedHashSet<>();
        if (idsPredecesoras.isEmpty()) {
            return resultado;
        }

        Set<Long> idsUnicos = new HashSet<>();
        for (Long idPredecesora : idsPredecesoras) {
            if (idPredecesora == null || !idsUnicos.add(idPredecesora)) {
                throw new PredecesoraInvalidaException(
                        "La lista de predecesoras contiene ids nulos o duplicados.");
            }
            if (idPredecesora.equals(idActividad)) {
                throw new PredecesoraInvalidaException(
                        "Una actividad no puede ser predecesora de si misma.");
            }
        }

        List<Actividad> encontradas = actividadRepository.findByIdsAndPlanificacion(
                idsUnicos, idPlanificacion);

        if (encontradas.size() != idsUnicos.size()) {
            throw new PredecesoraInvalidaException(
                    "Todas las predecesoras deben existir y pertenecer a la misma planificacion.");
        }

        resultado.addAll(encontradas);
        return resultado;
    }

    private void validarSinCiclos(Actividad actividad, List<Actividad> actividades) {
        Map<Long, Actividad> porId = new HashMap<>();
        for (Actividad candidata : actividades) {
            porId.put(candidata.getIdActividad(), candidata);
        }
        porId.put(actividad.getIdActividad(), actividad);

        if (tieneCiclo(actividad, porId, new HashSet<>(), new HashSet<>())) {
            throw new CicloActividadException(
                    "La actividad '" + actividad.getNombre()
                            + "' genera un ciclo de predecesoras.");
        }
    }

    private boolean tieneCiclo(
            Actividad actual,
            Map<Long, Actividad> porId,
            Set<Long> enCamino,
            Set<Long> completadas) {
        Long id = actual.getIdActividad();
        if (enCamino.contains(id)) {
            return true;
        }
        if (!completadas.add(id)) {
            return false;
        }

        enCamino.add(id);
        for (Actividad predecesora : actual.getPredecesoras()) {
            Actividad nodo = porId.get(predecesora.getIdActividad());
            if (nodo != null && tieneCiclo(nodo, porId, enCamino, completadas)) {
                return true;
            }
        }
        enCamino.remove(id);
        return false;
    }

    private Actividad guardarActividad(Actividad actividad) {
        try {
            return actividadRepository.saveAndFlush(actividad);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionPorCiclo(ex)) {
                throw new CicloActividadException(
                        "La modificacion genera un ciclo entre actividades.", ex);
            }
            throw ex;
        }
    }

    private boolean esViolacionPorCiclo(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            String mensaje = actual.getMessage();
            if (mensaje != null
                    && mensaje.toLowerCase(Locale.ROOT).contains("genera un ciclo")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private void actualizarRequerimientos(
            Actividad actividad,
            List<ModificarRequerimientoActividadRequest> requerimientos) {

        if (requerimientos == null) {
            throw new RequerimientoInvalidoException(
                    "La lista de requerimientos es obligatoria.");
        }

        for (ModificarRequerimientoActividadRequest requerimiento : requerimientos) {
            validarRequerimientoObligatorio(requerimiento);
        }

        List<RequerimientoActividad> existentes = requerimientoActividadRepository
                .findByActividadIdForUpdate(actividad.getIdActividad());

        Map<Long, RequerimientoActividad> porId = new HashMap<>();
        for (RequerimientoActividad existente : existentes) {
            porId.put(existente.getIdRequerimiento(), existente);
        }
        Map<Long, RequerimientoSnapshot> snapshotsActuales =
                snapshotsExistentes(existentes);

        Set<Long> idsSolicitados = new HashSet<>();
        Set<Long> idsSinCambios = new HashSet<>();
        for (ModificarRequerimientoActividadRequest requerimiento : requerimientos) {
            Long idRequerimiento = requerimiento.idRequerimiento();
            if (idRequerimiento == null) {
                continue;
            }

            if (!porId.containsKey(idRequerimiento)) {
                throw new RequerimientoInvalidoException(
                        "El requerimiento " + idRequerimiento
                                + " no pertenece a esta actividad.");
            }
            if (!idsSolicitados.add(idRequerimiento)) {
                throw new RequerimientoInvalidoException(
                        "El requerimiento " + idRequerimiento
                                + " esta duplicado en la solicitud.");
            }

            RequerimientoSnapshot snapshot = snapshotDesdeRequest(requerimiento);
            if (snapshot.equals(snapshotsActuales.get(idRequerimiento))) {
                idsSinCambios.add(idRequerimiento);
                continue;
            }
            if (requerimientoProteccionService.estaProtegido(idRequerimiento)) {
                throw new RequerimientoConVinculosException(idRequerimiento);
            }
        }

        LocalDateTime fechaHistorial = LocalDateTime.now(reloj);
        for (RequerimientoActividad existente : existentes) {
            if (idsSolicitados.contains(existente.getIdRequerimiento())) {
                continue;
            }
            if (requerimientoProteccionService.estaProtegido(
                    existente.getIdRequerimiento())) {
                // Política acordada: el requerimiento protegido omitido se conserva.
                continue;
            }
            requerimientoSnapshotService.asegurarHistorialActividad(
                    existente, fechaHistorial);
            requerimientoActividadService.eliminar(existente);
        }

        for (ModificarRequerimientoActividadRequest requerimiento : requerimientos) {
            CrearRequerimientoActividadRequest datos = new CrearRequerimientoActividadRequest(
                    requerimiento.cantidad(),
                    requerimiento.idProfesion(),
                    requerimiento.descripcion(),
                    requerimiento.caracteristicas(),
                    requerimiento.habilidades());

            if (requerimiento.idRequerimiento() == null) {
                requerimientoActividadService.persistir(actividad, datos);
            } else if (!idsSinCambios.contains(requerimiento.idRequerimiento())) {
                RequerimientoActividad existente =
                        porId.get(requerimiento.idRequerimiento());
                requerimientoSnapshotService.asegurarHistorialActividad(
                        existente, fechaHistorial);
                requerimientoActividadService.actualizar(existente, datos);
            }
        }
    }

    private void validarRequerimientoObligatorio(
            ModificarRequerimientoActividadRequest requerimiento) {
        if (requerimiento == null
                || requerimiento.cantidad() == null
                || requerimiento.idProfesion() == null
                || requerimiento.caracteristicas() == null
                || requerimiento.habilidades() == null) {
            throw new RequerimientoInvalidoException(
                    "Cada requerimiento debe incluir cantidad, profesion, caracteristicas y habilidades.");
        }
    }

    private Map<Long, RequerimientoSnapshot> snapshotsExistentes(
            List<RequerimientoActividad> requerimientos) {

        List<Long> idsCaracteristicas = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Set<Long>> valoresPorCaracteristica = new HashMap<>();
        if (!idsCaracteristicas.isEmpty()) {
            for (RequerimientoActCaractValor valor
                    : requerimientoActCaractValorRepository
                            .findByIdReqActCaractIn(idsCaracteristicas)) {
                valoresPorCaracteristica
                        .computeIfAbsent(
                                valor.getIdReqActCaract(),
                                id -> new HashSet<>())
                        .add(valor.getIdValor());
            }
        }

        Map<Long, RequerimientoSnapshot> resultado = new HashMap<>();
        for (RequerimientoActividad requerimiento : requerimientos) {
            Map<Long, CaracteristicaSnapshot> caracteristicas = new HashMap<>();
            for (RequerimientoActCaract caracteristica
                    : requerimiento.getCaracteristicas()) {
                Long idCaracteristica = caracteristica.getCaracteristica()
                        .getIdCaracteristica();
                Set<Long> valores = new HashSet<>(valoresPorCaracteristica
                        .getOrDefault(caracteristica.getIdReqActCaract(), Set.of()));
                caracteristicas.put(idCaracteristica, new CaracteristicaSnapshot(
                        idCaracteristica,
                        normalizar(caracteristica.getValorMin()),
                        normalizar(caracteristica.getValorMax()),
                        valores));
            }

            Set<Long> habilidades = new HashSet<>();
            if (requerimiento.getHabilidades() != null) {
                requerimiento.getHabilidades()
                        .forEach(habilidad -> habilidades.add(habilidad.getIdHabilidad()));
            }

            resultado.put(requerimiento.getIdRequerimiento(), new RequerimientoSnapshot(
                    requerimiento.getCantidad(),
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getIdProfesion()
                            : null,
                    normalizarTexto(requerimiento.getDescripcion()),
                    caracteristicas,
                    habilidades));
        }
        return resultado;
    }

    private RequerimientoSnapshot snapshotDesdeRequest(
            ModificarRequerimientoActividadRequest requerimiento) {

        Map<Long, CaracteristicaSnapshot> caracteristicas = new HashMap<>();
        if (requerimiento.caracteristicas() != null) {
            for (CrearRequerimientoCaractRequest caracteristica
                    : requerimiento.caracteristicas()) {
                Set<Long> valores = caracteristica.valores() == null
                        ? Set.of()
                        : new HashSet<>(caracteristica.valores());
                if (caracteristicas.put(
                        caracteristica.idCaracteristica(),
                        new CaracteristicaSnapshot(
                                caracteristica.idCaracteristica(),
                                normalizar(caracteristica.valorMin()),
                                normalizar(caracteristica.valorMax()),
                                valores)) != null) {
                    throw new RequerimientoInvalidoException(
                            "La caracteristica tecnica "
                                    + caracteristica.idCaracteristica()
                                    + " esta duplicada en el requerimiento.");
                }
            }
        }

        Set<Long> habilidades = requerimiento.habilidades() == null
                ? Set.of()
                : new HashSet<>(requerimiento.habilidades());

        return new RequerimientoSnapshot(
                requerimiento.cantidad(),
                requerimiento.idProfesion(),
                normalizarTexto(requerimiento.descripcion()),
                caracteristicas,
                habilidades);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private record CaracteristicaSnapshot(
            Long idCaracteristica,
            BigDecimal valorMin,
            BigDecimal valorMax,
            Set<Long> valores) {
    }

    private record RequerimientoSnapshot(
            Integer cantidad,
            Long idProfesion,
            String descripcion,
            Map<Long, CaracteristicaSnapshot> caracteristicas,
            Set<Long> habilidades) {
    }

    private void publicarActividadModificada(
            Actividad actividad,
            Proyecto proyecto,
            List<AsignacionActividad> asignaciones) {
        if (asignaciones.isEmpty()) {
            return;
        }

        Set<Long> idsPerfiles = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            idsPerfiles.add(asignacion.getMiembro().getPerfil().getIdPerfil());
        }

        eventPublisher.publishEvent(new ActividadModificadaEvent(
                actividad.getIdActividad(),
                proyecto.getIdProyecto(),
                actividad.getNombre(),
                proyecto.getNombre(),
                List.copyOf(idsPerfiles)));
    }
}
