package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ProyectoFiltroContratoResponse;
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
            boolean miFirmaPendiente,
            Pageable pageable);

    /**
     * Proyectos con al menos un contrato visible para el perfil activo, ya sea
     * como director activo o como parte firmante. Incluye proyectos históricos
     * en los que la membresía ya no está activa pero la firma persiste.
     */
    List<ProyectoFiltroContratoResponse> listarProyectosVisibles(
            Long idPerfilActivo);
}
