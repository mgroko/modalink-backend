package org.mgroko.backend.perfiles.dto;

import org.mgroko.backend.common.dto.PaginaResponse;

/**
 * Listado paginado de reseñas recibidas por un perfil junto con su resumen.
 */
public record ResenasPerfilResponse(
        Long idPerfil,
        ResumenResenasResponse resumen,
        PaginaResponse<ResenaPerfilResponse> calificaciones) {
}
