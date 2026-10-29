package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;

public record RequerimientoActividadResumenResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        Long cubiertos,
        Integer faltantes,
        Boolean satisfecho,
        Boolean puedePostularse,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades
) {

    /**
     * Constructor de compatibilidad para consumidores que aún no leen las
     * características ni las habilidades.
     */
    public RequerimientoActividadResumenResponse(
            Long idRequerimiento,
            Integer cantidad,
            String descripcion,
            Long idProfesion,
            String nombreProfesion,
            Long cubiertos,
            Integer faltantes,
            Boolean satisfecho,
            Boolean puedePostularse) {
        this(
                idRequerimiento,
                cantidad,
                descripcion,
                idProfesion,
                nombreProfesion,
                cubiertos,
                faltantes,
                satisfecho,
                puedePostularse,
                List.of(),
                List.of());
    }

    public RequerimientoActividadResumenResponse {
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        habilidades = habilidades == null ? List.of() : List.copyOf(habilidades);
    }
}
