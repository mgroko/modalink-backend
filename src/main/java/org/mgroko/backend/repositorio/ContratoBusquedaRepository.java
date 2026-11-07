package org.mgroko.backend.repositorio;

import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.repositorio.proyeccion.ContratoResumenProyeccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContratoBusquedaRepository {

    /**
     * Busqueda paginada de contratos visibles para el perfil activo, sin
     * cargar el texto legal. La visibilidad y los filtros se resuelven con
     * EXISTS para no duplicar filas.
     */
    Page<ContratoResumenProyeccion> buscar(
            BuscarContratosFiltro filtro,
            Long idPerfilActivo,
            Pageable pageable);
}
