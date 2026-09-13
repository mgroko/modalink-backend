package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.dto.BuscarPerfilesFiltro;
import org.mgroko.backend.perfiles.dto.PerfilBusquedaResponse;
import org.mgroko.backend.perfiles.especificacion.PerfilSpecifications;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarPerfilService {

    private final PerfilRepository perfilRepository;

    public BuscarPerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PerfilBusquedaResponse> buscarPerfiles(
            BuscarPerfilesFiltro filtro,
            int page,
            int size,
            boolean todos) {

        Pageable pageable;
        Sort sort = Sort.by(Sort.Direction.ASC, "nombreArtistico");

        if (todos || size <= 0) {
            // Se devuelven todos los resultados
            pageable = Pageable.unpaged(sort);
        } else {
            int paginaAjustada = Math.max(0, page);
            pageable = PageRequest.of(paginaAjustada, size, sort);
        }

        Specification<Perfil> spec = PerfilSpecifications.conFiltros(filtro);
        Page<Perfil> resultados = perfilRepository.findAll(spec, pageable);

        Page<PerfilBusquedaResponse> paginaDto = resultados.map(PerfilMapper::toBusquedaResponse);
        return PaginaResponse.fromPage(paginaDto);
    }
}
