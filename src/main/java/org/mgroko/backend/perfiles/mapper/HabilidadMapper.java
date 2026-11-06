package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;

public final class HabilidadMapper {

    private HabilidadMapper() {
    }

    public static HabilidadResponse toResponse(Habilidad habilidad) {
        return new HabilidadResponse(
                habilidad.getIdHabilidad(),
                habilidad.getCodigo(),
                habilidad.getNombre(),
                habilidad.getDescripcion());
    }
}
