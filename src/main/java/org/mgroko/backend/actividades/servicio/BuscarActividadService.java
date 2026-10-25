package org.mgroko.backend.actividades.servicio;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.actividades.especificacion.ActividadSpecifications;
import org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-52: búsqueda de actividades dentro de la planificación de un proyecto.
 */
@Service
public class BuscarActividadService {

    private final ActividadRepository actividadRepository;
    private final ProyectoRepository proyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public BuscarActividadService(
            ActividadRepository actividadRepository,
            ProyectoRepository proyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.actividadRepository = actividadRepository;
        this.proyectoRepository = proyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ActividadBusquedaResponse> buscar(
            Long idPerfilActivo,
            Long idProyecto,
            BuscarActividadesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        validarRangoDuracion(filtro);

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoVisibilidadService.validarAcceso(proyecto, idPerfilActivo);

        Pageable pageable = construirPageable(page, size, todos);
        Specification<Actividad> spec = ActividadSpecifications.conFiltros(idProyecto, filtro);
        Page<Actividad> resultados = actividadRepository.findAll(spec, pageable);
        List<Actividad> actividades = resultados.getContent();

        Map<Long, List<RequerimientoActividadResumenResponse>> requerimientos =
                buscarRequerimientos(actividades);
        Map<Long, List<ActividadVinculadaResponse>> predecesoras =
                buscarPredecesoras(actividades);

        List<ActividadBusquedaResponse> contenido = actividades.stream()
                .map(actividad -> ActividadMapper.toBusquedaResponse(
                        actividad,
                        proyecto,
                        requerimientos.getOrDefault(actividad.getIdActividad(), List.of()),
                        predecesoras.getOrDefault(actividad.getIdActividad(), List.of())))
                .toList();

        Page<ActividadBusquedaResponse> pagina = new PageImpl<>(
                contenido, resultados.getPageable(), resultados.getTotalElements());

        return PaginaResponse.fromPage(pagina);
    }

    private void validarRangoDuracion(BuscarActividadesFiltro filtro) {
        if (filtro == null) {
            return;
        }

        if ((filtro.duracionMin() != null && filtro.duracionMin() < 0)
                || (filtro.duracionMax() != null && filtro.duracionMax() < 0)) {
            throw new RangoDuracionInvalidoException(
                    "La duración mínima y máxima no pueden ser negativas.");
        }

        if (filtro.duracionMin() != null && filtro.duracionMax() != null
                && filtro.duracionMin() > filtro.duracionMax()) {
            throw new RangoDuracionInvalidoException(
                    "La duración mínima no puede ser mayor que la duración máxima.");
        }
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        Sort sort = Sort.by(Sort.Direction.ASC, "fechaHoraInicio")
                .and(Sort.by(Sort.Direction.ASC, "idActividad"));

        if (todos || size <= 0) {
            return Pageable.unpaged(sort);
        }

        return PageRequest.of(Math.max(0, page), size, sort);
    }

    private Map<Long, List<RequerimientoActividadResumenResponse>> buscarRequerimientos(
            List<Actividad> actividades) {

        List<Long> ids = actividades.stream()
                .map(Actividad::getIdActividad)
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return requerimientoActividadRepository.findConProfesionByActividadIds(ids).stream()
                .collect(Collectors.groupingBy(
                        requerimiento -> requerimiento.getActividad().getIdActividad(),
                        HashMap::new,
                        Collectors.mapping(
                                ActividadMapper::toRequerimientoResumen,
                                Collectors.toList())));
    }

    private Map<Long, List<ActividadVinculadaResponse>> buscarPredecesoras(
            List<Actividad> actividades) {

        List<Long> ids = actividades.stream()
                .map(Actividad::getIdActividad)
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ActividadVinculadaResponse>> resultado = new HashMap<>();

        for (Actividad actividad : actividadRepository.findConPredecesoras(ids)) {
            List<ActividadVinculadaResponse> predecesoras = actividad.getPredecesoras().stream()
                    .sorted(Comparator.comparing(Actividad::getNombre))
                    .map(predecesora -> new ActividadVinculadaResponse(
                            predecesora.getIdActividad(), predecesora.getNombre()))
                    .toList();
            resultado.put(actividad.getIdActividad(), predecesoras);
        }

        return resultado;
    }
}
