package org.mgroko.backend.perfiles.mapper;

import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;

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

        List<ValorCaracteristicaResponse> valores = caracteristica.getValores().stream()
                .map(ValorCaracteristicaMapper::toResponse)
                .toList();

        return new CaracteristicaTecnicaResponse(
                caracteristica.getIdCaracteristica(),
                caracteristica.getCodigo(),
                caracteristica.getUnidad(),
                idProfesion,
                nombreProfesion,
                caracteristica.getTipoDato(),
                valores);
    }
}