package org.mgroko.backend.home.dto;

import java.util.List;

import org.mgroko.backend.perfiles.dto.PerfilResponse;

public record HomeResumenResponse(
                PerfilResponse perfilActivo,
                List<Object> proyectosDestacados,
                List<Object> publicacionesRecientes) {
}
