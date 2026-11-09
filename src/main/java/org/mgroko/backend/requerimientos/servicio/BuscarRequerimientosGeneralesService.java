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
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
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
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.proyeccion.EstadoPostulacionProyeccion;
import org.mgroko.backend.repositorio.proyeccion.MiembroProyectoRolProyeccion;
import org.mgroko.backend.repositorio.proyeccion.PlanificacionEntregaProyeccion;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mgroko.backend.requerimientos.especificacion.RequerimientoGralOportunidadSpecifications;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-72: búsqueda global de requerimientos generales abiertos para postularse.
 *
 * <p>El proyecto no es obligatorio: sin filtros se listan todas las
 * oportunidades visibles, priorizando las de la profesión del perfil activo.
 * La elegibilidad es estructural y no incluye postulaciones previas ni
 * disponibilidad de calendario.</p>
 */
@Service
public class BuscarRequerimientosGeneralesService {

    private final RequerimientoGralProyectoRepository requerimientoGralRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ElegibilidadRequerimientoService elegibilidadRequerimientoService;

    public BuscarRequerimientosGeneralesService(
            RequerimientoGralProyectoRepository requerimientoGralRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            PostulacionGralRepository postulacionGralRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ElegibilidadRequerimientoService elegibilidadRequerimientoService) {
        this.requerimientoGralRepository = requerimientoGralRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
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
        Specification<RequerimientoGralProyecto> spec =
                RequerimientoGralOportunidadSpecifications.conFiltros(
                        idPerfilActivo, idProfesionPerfil, filtro);

        Page<RequerimientoGralProyecto> pagina =
                requerimientoGralRepository.findAll(spec, pageable);

        if (pagina.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pagina.getPageable(), pagina.getTotalElements()));
        }

        List<Long> ids = pagina.getContent().stream()
                .map(RequerimientoGralProyecto::getIdRequerimientoGral)
                .toList();

        List<RequerimientoGralProyecto> detalle =
                requerimientoGralRepository.findConDetalleByIds(ids);
        Map<Long, RequerimientoGralProyecto> porId = detalle.stream()
                .collect(Collectors.toMap(
                        RequerimientoGralProyecto::getIdRequerimientoGral,
                        requerimiento -> requerimiento));

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularGenerales(detalle);
        Map<Long, List<Long>> valoresPorCaracteristica =
                cargarValoresCaracteristicas(detalle);
        Map<Long, EstadoSolicitud> estados = cargarEstadosPostulacion(
                idPerfilActivo, ids);
        Set<Long> idsProyectos = idsProyectos(detalle);
        Map<Long, RolProyectoUsuario> roles = cargarRoles(
                idPerfilActivo, idsProyectos);
        Map<Long, LocalDate> fechasEntrega = cargarFechasEntrega(idsProyectos);

        List<RequerimientoOportunidadResponse> contenido = new ArrayList<>();
        for (RequerimientoGralProyecto itemPagina : pagina.getContent()) {
            RequerimientoGralProyecto requerimiento =
                    porId.get(itemPagina.getIdRequerimientoGral());
            if (requerimiento == null) {
                continue;
            }

            Long idProyecto = requerimiento.getProyecto().getIdProyecto();
            CupoRequerimiento cupo = cupos.get(requerimiento.getIdRequerimientoGral());
            if (cupo == null) {
                int cantidad = requerimiento.getCantidad() != null
                        ? requerimiento.getCantidad()
                        : 0;
                cupo = new CupoRequerimiento(cantidad, 0, cantidad, false);
            }

            ResultadoElegibilidad elegibilidad =
                    elegibilidadRequerimientoService.evaluarGeneral(
                            requerimiento.getProyecto(),
                            requerimiento,
                            cupo,
                            idProfesionPerfil,
                            roles.get(idProyecto));

            EstadoSolicitud estado = estados.get(
                    requerimiento.getIdRequerimientoGral());

            contenido.add(toResponse(
                    requerimiento,
                    cupo,
                    valoresPorCaracteristica,
                    elegibilidad,
                    estado,
                    fechasEntrega.get(idProyecto)));
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
            List<RequerimientoGralProyecto> requerimientos) {

        List<Long> ids = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoGralCaract::getIdReqGralCaract)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<Long>> valores = new HashMap<>();
        if (ids.isEmpty()) {
            return valores;
        }

        for (var valor : requerimientoGralCaractValorRepository
                .findByIdReqGralCaractIn(ids)) {
            valores.computeIfAbsent(valor.getIdReqGralCaract(),
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

        return postulacionGralRepository
                .findEstadosPorPerfilYRequerimientos(idPerfilActivo, idsRequerimientos)
                .stream()
                .collect(Collectors.toMap(
                        EstadoPostulacionProyeccion::getEntidadId,
                        EstadoPostulacionProyeccion::getEstado,
                        (actual, repetido) -> actual));
    }

    private Set<Long> idsProyectos(List<RequerimientoGralProyecto> requerimientos) {
        return requerimientos.stream()
                .map(RequerimientoGralProyecto::getProyecto)
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

    private Map<Long, LocalDate> cargarFechasEntrega(Set<Long> idsProyectos) {
        if (idsProyectos.isEmpty()) {
            return Map.of();
        }

        return planificacionRepository.findFechasEntregaPorProyectoIds(idsProyectos)
                .stream()
                .filter(proyeccion -> proyeccion.getIdProyecto() != null)
                .filter(proyeccion -> proyeccion.getFechaEntrega() != null)
                .collect(Collectors.toMap(
                        PlanificacionEntregaProyeccion::getIdProyecto,
                        PlanificacionEntregaProyeccion::getFechaEntrega,
                        (actual, repetido) -> actual));
    }

    private RequerimientoOportunidadResponse toResponse(
            RequerimientoGralProyecto requerimiento,
            CupoRequerimiento cupo,
            Map<Long, List<Long>> valoresPorCaracteristica,
            ResultadoElegibilidad elegibilidad,
            EstadoSolicitud estadoPostulacion,
            LocalDate fechaEntrega) {

        Proyecto proyecto = requerimiento.getProyecto();

        List<CaracteristicaRequerimientoResponse> caracteristicas =
                requerimiento.getCaracteristicas().stream()
                        .map(caracteristica -> new CaracteristicaRequerimientoResponse(
                                caracteristica.getCaracteristica().getIdCaracteristica(),
                                caracteristica.getCaracteristica().getCodigo(),
                                caracteristica.getCaracteristica().getTipoDato(),
                                caracteristica.getValorMin(),
                                caracteristica.getValorMax(),
                                valoresPorCaracteristica.getOrDefault(
                                        caracteristica.getIdReqGralCaract(), List.of())))
                        .toList();

        List<Long> habilidades = requerimiento.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        return new RequerimientoOportunidadResponse(
                TipoRequerimiento.GENERAL,
                requerimiento.getIdRequerimientoGral(),
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
                null,
                null,
                null,
                proyecto.getFechaInicio(),
                fechaEntrega,
                null,
                null,
                null,
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                caracteristicas,
                habilidades,
                elegibilidad.puedePostularse(),
                elegibilidad.motivo(),
                estadoPostulacion != null,
                estadoPostulacion,
                false,
                proyecto.getUbicacion() != null
                        ? UbicacionMapper.toCiudadResponse(proyecto.getUbicacion().getCiudad())
                        : null);
    }
}
