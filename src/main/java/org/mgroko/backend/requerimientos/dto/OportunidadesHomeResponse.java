package org.mgroko.backend.requerimientos.dto;

import java.util.List;

/**
 * Bloque de oportunidades del Home.
 */
public record OportunidadesHomeResponse(
        List<RequerimientoOportunidadResponse> oportunidades,
        long totalGenerales,
        long totalActividades,
        int limite
) {

    public OportunidadesHomeResponse {
        oportunidades = oportunidades == null ? List.of() : List.copyOf(oportunidades);
    }

    public static OportunidadesHomeResponse vacio(int limite) {
        return new OportunidadesHomeResponse(List.of(), 0, 0, limite);
    }
}
