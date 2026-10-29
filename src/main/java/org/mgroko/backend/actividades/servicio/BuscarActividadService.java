package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResumenResponse;
import org.mgroko.backend.actividades.especificacion.ActividadSpecifications;
import org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
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
 *
 * <p>Cada requerimiento expone su cupo y si el perfil activo puede postularse.
 * La elegibilidad es estructural (estado del proyecto y de la actividad,
 * profesión, cupo y rol); no incluye disponibilidad de calendario ni valida
 * postulaciones duplicadas.</p>
 */
@Service
public class BuscarActividadService {

    private final ActividadRepository actividadRepository;
    private final ProyectoRepository proyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;

    public BuscarActividadService(
            ActividadRepository actividadRepository,
            ProyectoRepository proyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService) {
        this.actividadRepository = actividadRepository;
        this.proyectoRepository = proyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
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
                actividades.isEmpty()
                        ? Map.of()
                        : buscarRequerimientos(
                                actividades,
                                proyecto,
                                cargarContextoElegibilidad(idProyecto, idPerfilActivo));

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

    private ContextoElegibilidad cargarContextoElegibilidad(
            Long idProyecto,
            Long idPerfilActivo) {

        Perfil perfil = perfilRepository.findByIdConProfesion(idPerfilActivo)
                .orElseThrow(PerfilNoEncontradoException::new);

        Long idProfesionPerfil = perfil.getProfesion() != null
                ? perfil.getProfesion().getIdProfesion()
                : null;

        MiembroProyecto membresia = miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, idPerfilActivo)
                .orElse(null);

        boolean puedeSerPostulante = membresia == null
                || RolProyectoUsuario.desde(membresia.getRolProyecto())
                        == RolProyectoUsuario.MIEMBRO;

        return new ContextoElegibilidad(idProfesionPerfil, puedeSerPostulante);
    }

    static void validarRangoDuracion(BuscarActividadesFiltro filtro) {
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
            List<Actividad> actividades,
            Proyecto proyecto,
            ContextoElegibilidad contexto) {

        List<Long> ids = actividades.stream()
                .map(Actividad::getIdActividad)
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        List<RequerimientoActividad> requerimientos =
                requerimientoActividadRepository.findConProfesionByActividadIds(ids);

        if (requerimientos.isEmpty()) {
            return Map.of();
        }

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularActividades(requerimientos);
        Map<Long, List<Long>> valoresPorRequerimientoCaract =
                cargarValoresCaracteristicas(requerimientos);

        Map<Long, List<RequerimientoActividadResumenResponse>> resultado = new HashMap<>();

        for (RequerimientoActividad requerimiento : requerimientos) {
            CupoRequerimiento cupo = cupos.get(requerimiento.getIdRequerimiento());
            if (cupo == null) {
                int cantidad = requerimiento.getCantidad() != null
                        ? requerimiento.getCantidad()
                        : 0;
                cupo = new CupoRequerimiento(cantidad, 0, cantidad, false);
            }

            boolean puedePostularse = ElegibilidadActividad.puedePostularse(
                    proyecto,
                    requerimiento,
                    cupo,
                    contexto.idProfesionPerfil(),
                    contexto.puedeSerPostulante());

            resultado.computeIfAbsent(
                            requerimiento.getActividad().getIdActividad(),
                            id -> new ArrayList<>())
                    .add(ActividadMapper.toRequerimientoResumen(
                            requerimiento, cupo, puedePostularse,
                            valoresPorRequerimientoCaract));
        }

        return resultado;
    }

    private Map<Long, List<Long>> cargarValoresCaracteristicas(
            List<RequerimientoActividad> requerimientos) {

        List<Long> idsReqActCaract = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (idsReqActCaract.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> valores = new HashMap<>();
        for (var valor : requerimientoActCaractValorRepository
                .findByIdReqActCaractIn(idsReqActCaract)) {
            valores.computeIfAbsent(valor.getIdReqActCaract(), id -> new ArrayList<>())
                    .add(valor.getIdValor());
        }

        valores.values().forEach(lista -> lista.sort(Comparator.naturalOrder()));
        return valores;
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

    private record ContextoElegibilidad(Long idProfesionPerfil, boolean puedeSerPostulante) {
    }
}
