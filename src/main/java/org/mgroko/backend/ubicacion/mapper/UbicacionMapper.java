package org.mgroko.backend.ubicacion.mapper;

import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

public final class UbicacionMapper {

    private UbicacionMapper() {
    }

    public static UbicacionResponse toResponse(Ubicacion ubicacion) {
        return new UbicacionResponse(
                ubicacion.getIdUbicacion(),
                ubicacion.getIdGeoref(),
                ubicacion.getLocalidad(),
                ubicacion.getProvincia(),
                ubicacion.getPais(),
                ubicacion.getCodigoPostal(),
                ubicacion.getLatitud(),
                ubicacion.getLongitud());
    }
}