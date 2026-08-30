package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.ProfesionResponse;

public class ProfesionMapper {

    private ProfesionMapper() {}

    public static ProfesionResponse toResponse(Profesion profesion) {
        return new ProfesionResponse(
                profesion.getIdProfesion(),
                profesion.getNombre(),
                profesion.getDescripcion());
    }
}