package org.mgroko.backend.perfiles.mapper;

import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;

public class PerfilMapper {

    private PerfilMapper() {}

    public static PerfilResponse toResponse(Perfil perfil) {
        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas().stream()
                .sorted(Comparator.comparing(c -> c.getCaracteristicaTecnica().getCodigo()))
                .map(PerfilMapper::toCaracteristicaResponse)
                .toList();

        return new PerfilResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado().name(),
                perfil.getProfesion().getNombre(),
                perfil.getFechaSolicitudBaja(),
                caracteristicas);
    }

    private static CaracteristicaResponse toCaracteristicaResponse(CaracteristicaPerfil cp) {
        ValorCaracteristica valor = cp.getValorCaracteristica();
        return new CaracteristicaResponse(
                cp.getCaracteristicaTecnica().getIdCaracteristica(),
                cp.getCaracteristicaTecnica().getCodigo(),
                cp.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getCodigo() : null,
                valor != null ? valor.getColorHex() : null);
    }
}