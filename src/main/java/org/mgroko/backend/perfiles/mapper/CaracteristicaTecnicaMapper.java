package org.mgroko.backend.perfiles.mapper;

import java.util.List;

import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;

public class CaracteristicaTecnicaMapper {

    private CaracteristicaTecnicaMapper() {}

    public static CaracteristicaTecnicaResponse toResponse(CaracteristicaTecnica caracteristica) {
        return toResponse(caracteristica, null);
    }

    /**
     * @param enUso flag de negocio: la característica está en uso por perfiles
     *              o requerimientos (sólo puede modificarse el nombre).
     *              {@code null} cuando el contexto no lo calcula (lado perfiles).
     */
    public static CaracteristicaTecnicaResponse toResponse(CaracteristicaTecnica caracteristica, Boolean enUso) {
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

        UnidadMedida unidad = caracteristica.getUnidadMedida();
        UnidadMedidaResponse unidadResponse = unidad != null
                ? new UnidadMedidaResponse(
                        unidad.getIdUnidad(),
                        unidad.getNombre(),
                        unidad.getSimbolo(),
                        unidad.getTipoDatoPermitido())
                : null;

        return new CaracteristicaTecnicaResponse(
                caracteristica.getIdCaracteristica(),
                caracteristica.getCodigo(),
                caracteristica.getNombre(),
                unidadResponse,
                idProfesion,
                nombreProfesion,
                caracteristica.getTipoDato(),
                valores,
                enUso);
    }
}