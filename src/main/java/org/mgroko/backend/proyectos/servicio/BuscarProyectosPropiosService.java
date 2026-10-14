package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.ActividadResumenResponse;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.proyectos.especificacion.ProyectoPropioSpecifications;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-29 bis: catálogo de proyectos propios del perfil activo
 * (asociados como director o miembro con participación activa).
 */
@Service
public class BuscarProyectosPropiosService {

    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ActividadRepository actividadRepository;

    public BuscarProyectosPropiosService(
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ActividadRepository actividadRepository) {
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProyectoPropioResponse> buscar(
            Long idPerfilActivo,
            BuscarProyectosPropiosFiltro filtro,
            int page,
            int size,
            boolean todos) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Pageable pageable;
        Sort sort = Sort.by(Sort.Direction.ASC, "nombre");

        if (todos || size <= 0) {
            // Se devuelven todos los resultados
            pageable = Pageable.unpaged(sort);
        } else {
            int paginaAjustada = Math.max(0, page);
            pageable = PageRequest.of(paginaAjustada, size, sort);
        }

        Specification<Proyecto> spec = ProyectoPropioSpecifications.conFiltros(idPerfilActivo, filtro);
        Page<Proyecto> resultados = proyectoRepository.findAll(spec, pageable);

        List<Proyecto> proyectos = resultados.getContent();
        final Map<Long, RolProyectoUsuario> roles;
        final Map<Long, ActividadResumenResponse> proximas;

        if (proyectos.isEmpty()) {
            roles = Map.of();
            proximas = Map.of();
        } else {
            List<Long> idsProyectos = proyectos.stream().map(Proyecto::getIdProyecto).toList();
            roles = buscarRoles(idPerfilActivo, idsProyectos);
            proximas = buscarProximasActividades(idPerfilActivo, idsProyectos);
        }

        Page<ProyectoPropioResponse> paginaDto = resultados.map(proyecto -> ProyectoMapper.toPropioResponse(
                proyecto,
                roles.get(proyecto.getIdProyecto()),
                proximas.get(proyecto.getIdProyecto())));
        return PaginaResponse.fromPage(paginaDto);
    }

    /** Rol del perfil activo en cada proyecto de la página (una sola consulta). */
    private Map<Long, RolProyectoUsuario> buscarRoles(Long idPerfilActivo, List<Long> idsProyectos) {
        return miembroProyectoRepository.buscarMiembrosActivosDeProyectos(idsProyectos, idPerfilActivo).stream()
                .collect(Collectors.toMap(
                        miembro -> miembro.getProyecto().getIdProyecto(),
                        miembro -> normalizarRol(miembro.getRolProyecto()),
                        (actual, repetido) -> actual));
    }

    /** Próxima actividad pendiente/en curso por proyecto de la página (una sola consulta). */
    private Map<Long, ActividadResumenResponse> buscarProximasActividades(Long idPerfilActivo, List<Long> idsProyectos) {
        return actividadRepository
                .findProximasActividadesAsignadas(idsProyectos, idPerfilActivo, LocalDateTime.now()).stream()
                .collect(Collectors.toMap(
                        actividad -> actividad.getPlanificacion().getProyecto().getIdProyecto(),
                        ProyectoMapper::toActividadResumen,
                        // Consulta ordenada por fecha: gana la primera (más próxima)
                        (actual, repetido) -> actual));
    }

    private RolProyectoUsuario normalizarRol(RolProyecto rol) {
        if (rol == null) {
            return null;
        }

        String codigo = rol.getCodigo() != null ? rol.getCodigo().trim().toUpperCase(Locale.ROOT) : "";
        if (RolProyectoUsuario.DIRECTOR.name().equals(codigo)) {
            return RolProyectoUsuario.DIRECTOR;
        }
        if (RolProyectoUsuario.MIEMBRO.name().equals(codigo)) {
            return RolProyectoUsuario.MIEMBRO;
        }

        // Fallback ante nombres de rol sin código (ej. datos de prueba)
        String nombre = rol.getNombre() != null ? rol.getNombre().trim().toLowerCase(Locale.ROOT) : "";
        return nombre.startsWith("director") ? RolProyectoUsuario.DIRECTOR : RolProyectoUsuario.MIEMBRO;
    }
}
