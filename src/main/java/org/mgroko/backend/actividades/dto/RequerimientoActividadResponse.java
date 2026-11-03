package org.mgroko.backend.actividades.dto;

import java.util.List;

import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;

public record RequerimientoActividadResponse(
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades,
        Long cubiertos,
        Integer faltantes,
        Boolean satisfecho,
        Boolean puedePostularse,
        Boolean protegido
) {

    public RequerimientoActividadResponse(
            Long idRequerimiento,
            Integer cantidad,
            String descripcion,
            Long idProfesion,
            String nombreProfesion,
            List<CaracteristicaRequerimientoResponse> caracteristicas,
            List<Long> habilidades,
            Long cubiertos,
            Integer faltantes,
            Boolean satisfecho,
            Boolean puedePostularse) {
        this(idRequerimiento, cantidad, descripcion, idProfesion, nombreProfesion,
                caracteristicas, habilidades, cubiertos, faltantes, satisfecho,
                puedePostularse, null);
    }

    public RequerimientoActividadResponse conProtegido(boolean estaProtegido) {
        return new RequerimientoActividadResponse(
                idRequerimiento,
                cantidad,
                descripcion,
                idProfesion,
                nombreProfesion,
                caracteristicas,
                habilidades,
                cubiertos,
                faltantes,
                satisfecho,
                puedePostularse,
                estaProtegido);
    }
}
