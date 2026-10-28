package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.actividades.dto.ActividadPlanificacionResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.especificacion.ActividadSpecifications;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tabla de Planificación: listado liviano de actividades de un proyecto.
 *
 * <p>No calcula CPM ni proyecciones. Muestra solo datos persistidos,
 * responsables activos y predecesoras. El orden es topológico por
 * dependencias con desempate por id, compartido con el cronograma.</p>
 */
@Service
public class ListarActividadesPlanificacionService {

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final AsignacionActividadRepository asignacionActividadRepository;

    public ListarActividadesPlanificacionService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            AsignacionActividadRepository asignacionActividadRepository) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ActividadPlanificacionResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            BuscarActividadesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        BuscarActividadService.validarRangoDuracion(filtro);

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        List<Actividad> todas = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
        Map<Long, Integer> posiciones = posiciones(OrdenActividades.topologico(todas));

        Specification<Actividad> spec = ActividadSpecifications.conFiltros(idProyecto, filtro);
        List<Actividad> filtradas = new ArrayList<>(
                actividadRepository.findAll(spec, Pageable.unpaged()).getContent());
        filtradas.sort(Comparator.comparingInt(
                actividad -> posiciones.getOrDefault(actividad.getIdActividad(), Integer.MAX_VALUE)));

        int total = filtradas.size();
        Pageable pageable = construirPageable(page, size, todos);
        List<Actividad> pagina = pageable.isPaged() ? paginar(filtradas, pageable) : filtradas;

        List<Long> ids = pagina.stream()
                .map(Actividad::getIdActividad)
                .toList();

        Map<Long, List<ActividadVinculadaResponse>> predecesoras = ids.isEmpty()
                ? Map.of()
                : cargarPredecesoras(ids);
        Map<Long, List<ProfesionalAsignadoResponse>> responsables = ids.isEmpty()
                ? Map.of()
                : cargarResponsables(ids);

        List<ActividadPlanificacionResponse> contenido = pagina.stream()
                .map(actividad -> ActividadMapper.toPlanificacionResponse(
                        actividad,
                        predecesoras.getOrDefault(actividad.getIdActividad(), List.of()),
                        responsables.getOrDefault(actividad.getIdActividad(), List.of())))
                .toList();

        Page<ActividadPlanificacionResponse> resultado =
                new PageImpl<>(contenido, pageable, total);
        return PaginaResponse.fromPage(resultado);
    }

    private Map<Long, Integer> posiciones(List<Actividad> ordenadas) {
        Map<Long, Integer> posiciones = new HashMap<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            posiciones.put(ordenadas.get(i).getIdActividad(), i);
        }
        return posiciones;
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        if (todos || size <= 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(Math.max(0, page), size);
    }

    private List<Actividad> paginar(List<Actividad> actividades, Pageable pageable) {
        int desde = (int) pageable.getOffset();
        if (desde >= actividades.size()) {
            return List.of();
        }
        int hasta = Math.min(desde + pageable.getPageSize(), actividades.size());
        return actividades.subList(desde, hasta);
    }

    private Map<Long, List<ActividadVinculadaResponse>> cargarPredecesoras(
            List<Long> ids) {

        Map<Long, List<ActividadVinculadaResponse>> resultado = new HashMap<>();
        for (Actividad actividad : actividadRepository.findConPredecesoras(ids)) {
            List<ActividadVinculadaResponse> predecesoras = actividad.getPredecesoras().stream()
                    .sorted(Comparator.comparing(
                            Actividad::getNombre,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .map(predecesora -> new ActividadVinculadaResponse(
                            predecesora.getIdActividad(), predecesora.getNombre()))
                    .toList();
            resultado.put(actividad.getIdActividad(), predecesoras);
        }
        return resultado;
    }

    private Map<Long, List<ProfesionalAsignadoResponse>> cargarResponsables(
            List<Long> ids) {

        Map<Long, Map<Long, ProfesionalAsignadoResponse>> porActividad = new HashMap<>();

        for (AsignacionActividad asignacion
                : asignacionActividadRepository.findActivasByActividadIds(ids)) {

            Long idActividad = asignacion.getActividad().getIdActividad();
            ProfesionalAsignadoResponse profesional =
                    ActividadMapper.toProfesionalAsignado(asignacion);

            if (profesional.idPerfil() == null) {
                continue;
            }

            porActividad
                    .computeIfAbsent(idActividad, id -> new LinkedHashMap<>())
                    .putIfAbsent(profesional.idPerfil(), profesional);
        }

        Map<Long, List<ProfesionalAsignadoResponse>> resultado = new HashMap<>();
        porActividad.forEach((idActividad, profesionales) -> {
            List<ProfesionalAsignadoResponse> lista = new ArrayList<>(profesionales.values());
            lista.sort(Comparator
                    .comparing(
                            ProfesionalAsignadoResponse::nombreArtistico,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(
                            ProfesionalAsignadoResponse::idPerfil,
                            Comparator.nullsLast(Long::compareTo)));
            resultado.put(idActividad, lista);
        });
        return resultado;
    }
}
