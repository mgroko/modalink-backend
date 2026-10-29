package org.mgroko.backend.postulaciones.dto;

import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;

/**
 * Página de UC-74 con los conteos por estado del proyecto.
 *
 * <p>Mantiene exactamente la forma de {@link PaginaResponse} y agrega
 * {@code conteosPorEstado} en el root, sin modificar el contrato compartido de
 * paginación.</p>
 */
public record PostulacionesPaginaResponse(
        List<PostulacionBusquedaResponse> contenido,
        int paginaActual,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima,
        ConteosPostulacionesResponse conteosPorEstado) {

    public static PostulacionesPaginaResponse from(
            PaginaResponse<PostulacionBusquedaResponse> pagina,
            ConteosPostulacionesResponse conteosPorEstado) {

        return new PostulacionesPaginaResponse(
                pagina.contenido(),
                pagina.paginaActual(),
                pagina.tamanoPagina(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                pagina.primera(),
                pagina.ultima(),
                conteosPorEstado);
    }
}
