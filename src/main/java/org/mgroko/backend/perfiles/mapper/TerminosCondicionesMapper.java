package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;

public final class TerminosCondicionesMapper {

    private TerminosCondicionesMapper() {
    }

    public static TerminosCondicionesResponse toResponse(Long idPerfil, PerfilTyc tyc) {
        return new TerminosCondicionesResponse(
                idPerfil,
                tyc.getIdTyc(),
                tyc.getDescripcion(),
                tyc.getFechaInicio(),
                tyc.getFechaFin(),
                false);
    }

    public static TerminosCondicionesResponse plantilla(Long idPerfil, String descripcion) {
        return new TerminosCondicionesResponse(
                idPerfil,
                null,
                descripcion,
                null,
                null,
                true);
    }
}
