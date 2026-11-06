package org.mgroko.backend.reemplazos.servicio.modelo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Requisito normalizado de una vacante de actividad o general.
 */
public record RequisitoReemplazo(
        Long idProfesion,
        String nombreProfesion,
        Set<Long> idsHabilidades,
        Map<Long, String> nombresHabilidades,
        List<CaracteristicaRequerida> caracteristicas) {

    public RequisitoReemplazo {
        idsHabilidades = idsHabilidades == null ? Set.of() : Set.copyOf(idsHabilidades);
        nombresHabilidades = nombresHabilidades == null
                ? Map.of()
                : Map.copyOf(nombresHabilidades);
        caracteristicas = caracteristicas == null
                ? List.of()
                : List.copyOf(caracteristicas);
    }
}
