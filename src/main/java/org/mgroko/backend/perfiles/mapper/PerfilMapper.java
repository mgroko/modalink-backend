package org.mgroko.backend.perfiles.mapper;

import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;

public class PerfilMapper {

    private PerfilMapper() {}

    public static PerfilResponse toResponse(Perfil perfil) {
        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas().stream()
                .sorted(Comparator.comparing(c -> c.getCaracteristicaTecnica().getCodigo()))
                .map(c -> new CaracteristicaResponse(
                        c.getCaracteristicaTecnica().getIdCaracteristica(),
                        c.getCaracteristicaTecnica().getCodigo(),
                        c.getValor()))
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
}