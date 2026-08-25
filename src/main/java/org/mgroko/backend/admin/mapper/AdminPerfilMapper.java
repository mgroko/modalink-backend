package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.AdminPerfilResponse;
import org.mgroko.backend.modelo.Perfil;

public class AdminPerfilMapper {

    private AdminPerfilMapper() {}

    public static AdminPerfilResponse toResponse(Perfil perfil) {
        return new AdminPerfilResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado().name(),
                perfil.getProfesion().getNombre(),
                perfil.getFechaSolicitudBaja()
        );
    }
}
