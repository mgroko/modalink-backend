package org.mgroko.backend.postulaciones.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.ConteosPostulacionesResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionesPaginaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.especificacion.PostulacionSpecifications;
import org.mgroko.backend.postulaciones.exception.RangoFechasPostulacionInvalidoException;
import org.mgroko.backend.postulaciones.mapper.PostulacionMapper;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-74: búsqueda de postulaciones de un proyecto.
 *
 * <p>La consulta se hace por tipo (general/actividad) y los resultados se
 * unen, ordenan y paginan en memoria. Es una búsqueda acotada al proyecto,
 * pensada para el volumen de postulaciones de una convocatoria.</p>
 */
@Service
public class BuscarPostulacionesProyectoService {

    static final String PERMISO_GESTIONAR_POSTULACIONES = "GESTIONAR_POSTULACIONES";

    private final ProyectoRepository proyectoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;

    public BuscarPostulacionesProyectoService(
            ProyectoRepository proyectoRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService) {
        this.proyectoRepository = proyectoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public PostulacionesPaginaResponse buscar(
            Long idPerfilActivo,
            Long idProyecto,
            BuscarPostulacionesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        validarFechas(filtro);

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_GESTIONAR_POSTULACIONES);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        // Los conteos necesitan los tres estados: se consulta sin el filtro
        // estado y luego se aplica ese filtro solo al contenido paginado.
        BuscarPostulacionesFiltro filtroSinEstado =
                filtro != null ? filtro.sinEstado() : null;

        boolean incluyeGenerales = incluyeTipo(filtroSinEstado, TipoPostulacion.GENERAL);
        boolean incluyeActividades = incluyeTipo(filtroSinEstado, TipoPostulacion.ACTIVIDAD);

        Map<Long, MiembroProyecto> miembrosActivos = miembrosActivosPorPerfil(idProyecto);
        Set<Long> perfilesDirectores = incluyeActividades
                ? perfilesDirectores(idProyecto)
                : Set.of();
        Set<String> asignacionesActivas = incluyeActividades
                ? asignacionesActivas(idProyecto)
                : Set.of();

        List<PostulacionBusquedaResponse> resultados = new ArrayList<>();

        if (incluyeGenerales) {
            agregarGenerales(resultados, proyecto, filtroSinEstado, miembrosActivos);
        }

        if (incluyeActividades) {
            agregarActividades(resultados, proyecto, filtroSinEstado,
                    miembrosActivos, perfilesDirectores, asignacionesActivas);
        }

        ConteosPostulacionesResponse conteos = contar(resultados);
        aplicarFiltroEstado(resultados, filtro);
        resultados.sort(comparador());

        return PostulacionesPaginaResponse.from(
                paginar(resultados, page, size, todos), conteos);
    }

    private void agregarGenerales(
            List<PostulacionBusquedaResponse> resultados,
            Proyecto proyecto,
            BuscarPostulacionesFiltro filtro,
            Map<Long, MiembroProyecto> miembrosActivos) {

        Map<Long, CupoRequerimiento> cupos = new HashMap<>();
        boolean proyectoHabilitado = proyectoHabilitado(proyecto);

        for (PostulacionGral postulacion : postulacionGralRepository.findAll(
                PostulacionSpecifications.generales(proyecto.getIdProyecto(), filtro))) {

            Long idPerfil = postulacion.getPerfil().getIdPerfil();
            boolean esMiembro = miembrosActivos.containsKey(idPerfil);
            boolean perfilActivo = postulacion.getPerfil().getEstado() == EstadoPerfil.ACTIVO;

            CupoRequerimiento cupo = cupos.computeIfAbsent(
                    postulacion.getRequerimientoGral().getIdRequerimientoGral(),
                    id -> cupoRequerimientoService.calcularGeneral(
                            postulacion.getRequerimientoGral()));

            boolean pendiente = postulacion.getEstado() == EstadoSolicitud.PENDIENTE;
            boolean puedeAceptar = pendiente
                    && proyectoHabilitado
                    && perfilActivo
                    && !cupo.satisfecho()
                    && !esMiembro;
            boolean puedeRechazar = pendiente && proyectoHabilitado;
            boolean aportaCupo = postulacion.getEstado() == EstadoSolicitud.ACEPTADA
                    && esMiembro;

            resultados.add(PostulacionMapper.toBusquedaGeneral(
                    postulacion, cupo, aportaCupo, puedeAceptar, puedeRechazar));
        }
    }

    private void agregarActividades(
            List<PostulacionBusquedaResponse> resultados,
            Proyecto proyecto,
            BuscarPostulacionesFiltro filtro,
            Map<Long, MiembroProyecto> miembrosActivos,
            Set<Long> perfilesDirectores,
            Set<String> asignacionesActivas) {

        Map<Long, CupoRequerimiento> cupos = new HashMap<>();
        boolean proyectoHabilitado = proyectoHabilitado(proyecto);

        for (PostulacionActividad postulacion : postulacionActividadRepository.findAll(
                PostulacionSpecifications.actividades(proyecto.getIdProyecto(), filtro))) {

            Long idPerfil = postulacion.getPerfil().getIdPerfil();
            MiembroProyecto miembro = miembrosActivos.get(idPerfil);
            boolean esMiembro = miembro != null;
            boolean esDirector = perfilesDirectores.contains(idPerfil);
            boolean perfilActivo = postulacion.getPerfil().getEstado() == EstadoPerfil.ACTIVO;
            boolean actividadPendiente = postulacion.getRequerimiento()
                    .getActividad().getEstado() == EstadoActividad.PENDIENTE;

            CupoRequerimiento cupo = cupos.computeIfAbsent(
                    postulacion.getRequerimiento().getIdRequerimiento(),
                    id -> cupoRequerimientoService.calcularActividad(
                            postulacion.getRequerimiento()));

            String claveAsignacion = postulacion.getRequerimiento().getIdRequerimiento()
                    + ":" + (miembro != null ? miembro.getIdMiembro() : null);
            boolean yaAsignado = esMiembro && asignacionesActivas.contains(claveAsignacion);

            boolean pendiente = postulacion.getEstado() == EstadoSolicitud.PENDIENTE;
            boolean miembroApto = !esMiembro || !esDirector;
            boolean puedeAceptar = pendiente
                    && proyectoHabilitado
                    && perfilActivo
                    && actividadPendiente
                    && !cupo.satisfecho()
                    && miembroApto
                    && !yaAsignado;
            boolean puedeRechazar = pendiente && proyectoHabilitado;
            boolean aportaCupo = postulacion.getEstado() == EstadoSolicitud.ACEPTADA
                    && esMiembro
                    && yaAsignado;

            resultados.add(PostulacionMapper.toBusquedaActividad(
                    postulacion, cupo, aportaCupo, puedeAceptar, puedeRechazar));
        }
    }

    private boolean incluyeTipo(BuscarPostulacionesFiltro filtro, TipoPostulacion tipo) {
        return filtro == null || filtro.tipo() == null || filtro.tipo() == tipo;
    }

    private boolean proyectoHabilitado(Proyecto proyecto) {
        return proyecto.getEstado() == EstadoProyecto.PUBLICADO
                || proyecto.getEstado() == EstadoProyecto.CONFIRMADO;
    }

    private Map<Long, MiembroProyecto> miembrosActivosPorPerfil(Long idProyecto) {
        return miembroProyectoRepository.buscarMiembrosActivosDeProyecto(idProyecto).stream()
                .collect(Collectors.toMap(
                        miembro -> miembro.getPerfil().getIdPerfil(),
                        miembro -> miembro,
                        (actual, repetido) -> actual));
    }

    private Set<Long> perfilesDirectores(Long idProyecto) {
        return miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(idProyecto)).stream()
                .filter(miembro -> miembro.getPerfil() != null)
                .map(miembro -> miembro.getPerfil().getIdPerfil())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<String> asignacionesActivas(Long idProyecto) {
        return asignacionActividadRepository
                .findActivasDeProyectoConRequerimiento(idProyecto).stream()
                .map(asignacion -> asignacion.getRequerimientoActividad().getIdRequerimiento()
                        + ":" + asignacion.getMiembro().getIdMiembro())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ConteosPostulacionesResponse contar(
            List<PostulacionBusquedaResponse> resultados) {

        long pendiente = 0;
        long aceptada = 0;
        long rechazada = 0;

        for (PostulacionBusquedaResponse postulacion : resultados) {
            if (postulacion.estado() == null) {
                continue;
            }
            switch (postulacion.estado()) {
                case PENDIENTE -> pendiente++;
                case ACEPTADA -> aceptada++;
                case RECHAZADA -> rechazada++;
            }
        }

        return new ConteosPostulacionesResponse(
                pendiente, aceptada, rechazada, pendiente + aceptada + rechazada);
    }

    private void aplicarFiltroEstado(
            List<PostulacionBusquedaResponse> resultados,
            BuscarPostulacionesFiltro filtro) {

        if (filtro == null || filtro.estado() == null || filtro.estado().isEmpty()) {
            return;
        }

        Set<EstadoSolicitud> estados = new HashSet<>(filtro.estado());
        resultados.removeIf(postulacion -> !estados.contains(postulacion.estado()));
    }

    private void validarFechas(BuscarPostulacionesFiltro filtro) {
        if (filtro != null
                && filtro.fechaDesde() != null
                && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasPostulacionInvalidoException();
        }
    }

    private Comparator<PostulacionBusquedaResponse> comparador() {
        return Comparator
                .comparingInt((PostulacionBusquedaResponse postulacion) ->
                        prioridad(postulacion.estado()))
                .thenComparing(
                        PostulacionBusquedaResponse::fechaPostulacion,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        PostulacionBusquedaResponse::idPostulacion,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int prioridad(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE -> 0;
            case ACEPTADA -> 1;
            case RECHAZADA -> 2;
        };
    }

    private PaginaResponse<PostulacionBusquedaResponse> paginar(
            List<PostulacionBusquedaResponse> resultados,
            int page,
            int size,
            boolean todos) {

        if (todos || size <= 0) {
            Page<PostulacionBusquedaResponse> pagina = new PageImpl<>(resultados);
            return PaginaResponse.fromPage(pagina);
        }

        int paginaAjustada = Math.max(0, page);
        int desde = Math.min(paginaAjustada * size, resultados.size());
        int hasta = Math.min(desde + size, resultados.size());

        Page<PostulacionBusquedaResponse> pagina = new PageImpl<>(
                resultados.subList(desde, hasta),
                PageRequest.of(paginaAjustada, size),
                resultados.size());

        return PaginaResponse.fromPage(pagina);
    }
}
