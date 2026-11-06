package org.mgroko.backend.requerimientos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.proyeccion.EstadoPostulacionProyeccion;
import org.mgroko.backend.repositorio.proyeccion.MiembroProyectoRolProyeccion;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mgroko.backend.requerimientos.especificacion.RequerimientoActividadOportunidadSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-73: búsqueda global de requerimientos de actividad abiertos para
 * postularse.
 *
 * <p>No reemplaza la búsqueda por proyecto que se usa para invitar: este
 * servicio recorre todos los proyectos visibles para el perfil activo.</p>
 */
@Service
public class BuscarRequerimientosActividadGlobalService {

    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PerfilRepository perfilRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ElegibilidadRequerimientoService elegibilidadRequerimientoService;

    public BuscarRequerimientosActividadGlobalService(
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PerfilRepository perfilRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ElegibilidadRequerimientoService elegibilidadRequerimientoService) {
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.perfilRepository = perfilRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.elegibilidadRequerimientoService = elegibilidadRequerimientoService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RequerimientoOportunidadResponse> buscar(
            Long idPerfilActivo,
            BuscarRequerimientosGlobalFiltro filtro,
            int page,
            int size,
            boolean todos) {

        Perfil perfil = cargarPerfilActivo(idPerfilActivo);
        Long idProfesionPerfil = perfil.getProfesion() != null
                ? perfil.getProfesion().getIdProfesion()
                : null;

        Pageable pageable = construirPageable(page, size, todos);
        Specification<RequerimientoActividad> spec =
                RequerimientoActividadOportunidadSpecifications.conFiltros(
                        idPerfilActivo, idProfesionPerfil, filtro);

        Page<RequerimientoActividad> pagina =
                requerimientoActividadRepository.findAll(spec, pageable);

        if (pagina.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pagina.getPageable(), pagina.getTotalElements()));
        }

        List<Long> ids = pagina.getContent().stream()
                .map(RequerimientoActividad::getIdRequerimiento)
                .toList();

        List<RequerimientoActividad> detalle =
                requerimientoActividadRepository.findConDetalleByIds(ids);
        Map<Long, RequerimientoActividad> porId = detalle.stream()
                .collect(Collectors.toMap(
                        RequerimientoActividad::getIdRequerimiento,
                        requerimiento -> requerimiento));

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularActividades(detalle);
        Map<Long, List<Long>> valoresPorCaracteristica =
                cargarValoresCaracteristicas(detalle);
        Map<Long, EstadoSolicitud> estados = cargarEstadosPostulacion(
                idPerfilActivo, ids);
        Set<Long> idsProyectos = idsProyectos(detalle);
        Map<Long, RolProyectoUsuario> roles = cargarRoles(
                idPerfilActivo, idsProyectos);

        List<RequerimientoOportunidadResponse> contenido = new ArrayList<>();
        for (RequerimientoActividad itemPagina : pagina.getContent()) {
            RequerimientoActividad requerimiento =
                    porId.get(itemPagina.getIdRequerimiento());
            if (requerimiento == null) {
                continue;
            }

            Actividad actividad = requerimiento.getActividad();
            Planificacion planificacion = actividad != null
                    ? actividad.getPlanificacion()
                    : null;
            Proyecto proyecto = planificacion != null
                    ? planificacion.getProyecto()
                    : null;

            if (proyecto == null) {
                continue;
            }

            CupoRequerimiento cupo = cupos.get(requerimiento.getIdRequerimiento());
            if (cupo == null) {
                int cantidad = requerimiento.getCantidad() != null
                        ? requerimiento.getCantidad()
                        : 0;
                cupo = new CupoRequerimiento(cantidad, 0, cantidad, false);
            }

            ResultadoElegibilidad elegibilidad =
                    elegibilidadRequerimientoService.evaluarActividad(
                            proyecto,
                            requerimiento,
                            cupo,
                            idProfesionPerfil,
                            roles.get(proyecto.getIdProyecto()));

            EstadoSolicitud estado = estados.get(
                    requerimiento.getIdRequerimiento());

            contenido.add(toResponse(
                    requerimiento,
                    actividad,
                    proyecto,
                    cupo,
                    valoresPorCaracteristica,
                    elegibilidad,
                    estado));
        }

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pagina.getPageable(), pagina.getTotalElements()));
    }

    private Perfil cargarPerfilActivo(Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdConProfesion(idPerfilActivo)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
        return perfil;
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        if (todos || size <= 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(Math.max(0, page), size);
    }

    private Map<Long, List<Long>> cargarValoresCaracteristicas(
            List<RequerimientoActividad> requerimientos) {

        List<Long> ids = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<Long>> valores = new HashMap<>();
        if (ids.isEmpty()) {
            return valores;
        }

        for (var valor : requerimientoActCaractValorRepository
                .findByIdReqActCaractIn(ids)) {
            valores.computeIfAbsent(valor.getIdReqActCaract(),
                    id -> new ArrayList<>()).add(valor.getIdValor());
        }
        valores.values().forEach(lista -> lista.sort(Comparator.naturalOrder()));
        return valores;
    }

    private Map<Long, EstadoSolicitud> cargarEstadosPostulacion(
            Long idPerfilActivo,
            List<Long> idsRequerimientos) {

        if (idsRequerimientos.isEmpty()) {
            return Map.of();
        }

        return postulacionActividadRepository
                .findEstadosPorPerfilYRequerimientos(idPerfilActivo, idsRequerimientos)
                .stream()
                .collect(Collectors.toMap(
                        EstadoPostulacionProyeccion::getEntidadId,
                        EstadoPostulacionProyeccion::getEstado,
                        (actual, repetido) -> actual));
    }

    private Set<Long> idsProyectos(List<RequerimientoActividad> requerimientos) {
        return requerimientos.stream()
                .map(RequerimientoActividad::getActividad)
                .filter(Objects::nonNull)
                .map(Actividad::getPlanificacion)
                .filter(Objects::nonNull)
                .map(Planificacion::getProyecto)
                .filter(Objects::nonNull)
                .map(Proyecto::getIdProyecto)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Long, RolProyectoUsuario> cargarRoles(
            Long idPerfilActivo,
            Set<Long> idsProyectos) {

        if (idsProyectos.isEmpty()) {
            return Map.of();
        }

        return miembroProyectoRepository
                .findRolesActivosPorPerfilYProyectos(idsProyectos, idPerfilActivo)
                .stream()
                .collect(Collectors.toMap(
                        MiembroProyectoRolProyeccion::getIdProyecto,
                        rol -> RolProyectoUsuario.desde(rol.getCodigo(), rol.getNombre()),
                        (actual, repetido) -> actual));
    }

    private RequerimientoOportunidadResponse toResponse(
            RequerimientoActividad requerimiento,
            Actividad actividad,
            Proyecto proyecto,
            CupoRequerimiento cupo,
            Map<Long, List<Long>> valoresPorCaracteristica,
            ResultadoElegibilidad elegibilidad,
            EstadoSolicitud estadoPostulacion) {

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

        LocalDate fechaEntrega = actividad.getPlanificacion() != null
                ? actividad.getPlanificacion().getFechaEntrega()
                : null;

        return new RequerimientoOportunidadResponse(
                TipoRequerimiento.ACTIVIDAD,
                requerimiento.getIdRequerimiento(),
                requerimiento.getCantidad(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getIdProfesion()
                        : null,
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getNombre()
                        : null,
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getEstado() != null ? proyecto.getEstado().getNombre() : null,
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getEstado() != null ? actividad.getEstado().name() : null,
                proyecto.getFechaInicio(),
                fechaEntrega,
                actividad.getFechaHoraInicio(),
                actividad.getFechaHoraFin(),
                actividad.getDuracionMinutos(),
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                caracteristicas,
                habilidades,
                elegibilidad.puedePostularse(),
                elegibilidad.motivo(),
                estadoPostulacion != null,
                estadoPostulacion,
                true);
    }
}
