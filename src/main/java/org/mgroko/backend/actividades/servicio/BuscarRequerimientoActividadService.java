package org.mgroko.backend.actividades.servicio;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadBusquedaResponse;
import org.mgroko.backend.actividades.especificacion.RequerimientoActividadSpecifications;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
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
 * UC-73: búsqueda de requerimientos de actividad dentro de un proyecto.
 *
 * <p>El proyecto es el criterio mínimo obligatorio. Los demás filtros son
 * opcionales y se combinan entre sí. La visibilidad se delega a la regla
 * compartida de proyectos.</p>
 */
@Service
public class BuscarRequerimientoActividadService {

    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public BuscarRequerimientoActividadService(
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RequerimientoActividadBusquedaResponse> buscar(
            Long idPerfilActivo,
            Long idProyecto,
            BuscarRequerimientosActividadFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdConProfesion(idPerfilActivo)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoVisibilidadService.validarAcceso(proyecto, idPerfilActivo);

        Pageable pageable = construirPageable(page, size, todos);
        Specification<RequerimientoActividad> spec =
                RequerimientoActividadSpecifications.conFiltros(idProyecto, filtro);
        Page<RequerimientoActividad> pagina =
                requerimientoActividadRepository.findAll(spec, pageable);

        List<Long> ids = pagina.getContent().stream()
                .map(RequerimientoActividad::getIdRequerimiento)
                .toList();

        if (ids.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pagina.getPageable(), pagina.getTotalElements()));
        }

        List<RequerimientoActividad> detalle =
                requerimientoActividadRepository.findConDetalleByIds(ids);
        Map<Long, List<Long>> valoresPorCaracteristica = cargarValoresCaracteristicas(detalle);

        List<RequerimientoActividadBusquedaResponse> contenido = detalle.stream()
                .map(requerimiento -> toResponse(requerimiento, valoresPorCaracteristica))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pagina.getPageable(), pagina.getTotalElements()));
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        Sort sort = Sort.by(Sort.Direction.ASC, "idRequerimiento");
        if (todos || size <= 0) {
            return Pageable.unpaged(sort);
        }
        return PageRequest.of(Math.max(0, page), size, sort);
    }

    private Map<Long, List<Long>> cargarValoresCaracteristicas(
            List<RequerimientoActividad> requerimientos) {

        List<Long> ids = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> valores = new HashMap<>();
        for (var valor : requerimientoActCaractValorRepository.findByIdReqActCaractIn(ids)) {
            valores.computeIfAbsent(valor.getIdReqActCaract(),
                    id -> new java.util.ArrayList<>()).add(valor.getIdValor());
        }
        valores.values().forEach(lista -> lista.sort(Comparator.naturalOrder()));
        return valores;
    }

    private RequerimientoActividadBusquedaResponse toResponse(
            RequerimientoActividad requerimiento,
            Map<Long, List<Long>> valoresPorCaracteristica) {

        var actividad = requerimiento.getActividad();
        var proyecto = actividad.getPlanificacion().getProyecto();

        List<CaracteristicaRequerimientoResponse> caracteristicas =
                requerimiento.getCaracteristicas().stream()
                        .map(caracteristica -> new CaracteristicaRequerimientoResponse(
                                caracteristica.getCaracteristica().getIdCaracteristica(),
                                caracteristica.getCaracteristica().getCodigo(),
                                caracteristica.getCaracteristica().getTipoDato(),
                                caracteristica.getValorMin(),
                                caracteristica.getValorMax(),
                                valoresPorCaracteristica.getOrDefault(
                                        caracteristica.getIdReqActCaract(), List.of())))
                        .toList();

        List<Long> habilidades = requerimiento.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        return new RequerimientoActividadBusquedaResponse(
                requerimiento.getIdRequerimiento(),
                requerimiento.getCantidad(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getIdProfesion()
                        : null,
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getNombre()
                        : null,
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                caracteristicas,
                habilidades);
    }
}
