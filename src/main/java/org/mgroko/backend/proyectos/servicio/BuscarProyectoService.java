package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.dto.ProyectoBusquedaResponse;
import org.mgroko.backend.proyectos.especificacion.ProyectoSpecifications;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;

    public BuscarProyectoService(
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository) {
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProyectoBusquedaResponse> buscarProyectos(
            Long idPerfilActivo,
            BuscarProyectosFiltro filtro,
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

        Specification<Proyecto> spec = ProyectoSpecifications.conFiltros(filtro, idPerfilActivo);
        Page<Proyecto> resultados = proyectoRepository.findAll(spec, pageable);

        Map<Long, Perfil> directores = buscarDirectores(resultados.getContent());

        Page<ProyectoBusquedaResponse> paginaDto = resultados.map(
                proyecto -> ProyectoMapper.toBusquedaResponse(
                        proyecto, directores.get(proyecto.getIdProyecto())));
        return PaginaResponse.fromPage(paginaDto);
    }

    /** Resuelve los directores activos de la página en una sola consulta (sin N+1). */
    private Map<Long, Perfil> buscarDirectores(List<Proyecto> proyectos) {
        if (proyectos.isEmpty()) {
            return Map.of();
        }

        List<Long> idsProyectos = proyectos.stream().map(Proyecto::getIdProyecto).toList();

        return miembroProyectoRepository.buscarDirectoresDeProyectos(idsProyectos).stream()
                .collect(Collectors.toMap(
                        miembro -> miembro.getProyecto().getIdProyecto(),
                        MiembroProyecto::getPerfil,
                        (actual, repetido) -> actual));
    }
}
