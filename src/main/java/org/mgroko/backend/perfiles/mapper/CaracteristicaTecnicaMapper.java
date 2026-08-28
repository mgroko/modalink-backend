package org.mgroko.backend.perfiles.mapper;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;

public class CaracteristicaTecnicaMapper {

    private CaracteristicaTecnicaMapper() {}

    public static CaracteristicaTecnicaResponse toResponse(CaracteristicaTecnica caracteristica) {
        Long idProfesion = null;
        String nombreProfesion = null;
        Profesion profesion = caracteristica.getProfesion();
        if (profesion != null) {
            idProfesion = profesion.getIdProfesion();
            nombreProfesion = profesion.getNombre();
        }

        return new CaracteristicaTecnicaResponse(
                caracteristica.getIdCaracteristica(),
                caracteristica.getCodigo(),
                caracteristica.getUnidad(),
                idProfesion,
                nombreProfesion);
    }
}