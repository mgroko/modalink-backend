package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.PerfilDetalleResponse;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerPerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    public VerPerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilDetalleResponse obtenerDetalle(Long idPerfil, Long idUsuarioAutenticado) {
        Usuario usuarioAutenticado = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuarioAutenticado.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        boolean esPropietario = perfil.getUsuario().getIdUsuario().equals(idUsuarioAutenticado);

        if (esPropietario) {
            // Si el perfil está en Baja definitiva, no está disponible
            if (perfil.getEstado() == EstadoPerfil.Baja) {
                throw new PerfilNoEncontradoException("Perfil no encontrado.");
            }
            return PerfilMapper.toDetalleResponse(perfil, true);
        }

        // Si es un tercero viendo el perfil:
        // 1. El perfil debe estar Activo
        // 2. El usuario propietario debe estar Activo (no PendienteBaja, Deshabilitado ni Baja)
        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilNoEncontradoException("Perfil no encontrado.");
        }

        if (perfil.getUsuario().getEstado() != EstadoUsuario.Activo) {
            throw new PerfilNoEncontradoException("Perfil no encontrado.");
        }

        return PerfilMapper.toDetalleResponse(perfil, false);
    }
}
