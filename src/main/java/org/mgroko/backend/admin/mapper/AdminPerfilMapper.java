package org.mgroko.backend.admin.mapper;

import org.mgroko.backend.admin.dto.AdminPerfilResponse;
import org.mgroko.backend.admin.dto.AdminPerfilResumenResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;

public class AdminPerfilMapper {

    private AdminPerfilMapper() {}

    public static AdminPerfilResumenResponse toResumen(Perfil perfil) {
        Usuario usuario = perfil.getUsuario();
        return new AdminPerfilResumenResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getEstado() != null ? perfil.getEstado().name() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null,
                usuario != null ? usuario.getIdUsuario() : null,
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getApellido() : null,
                usuario != null ? usuario.getCorreo() : null,
                perfil.getFechaSolicitudBaja(),
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null
        );
    }

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
