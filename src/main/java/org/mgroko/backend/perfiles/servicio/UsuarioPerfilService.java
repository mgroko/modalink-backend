package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminPerfilResponse;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.mapper.AdminPerfilMapper;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioPerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    public UsuarioPerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPerfilResponse> listarPerfiles(Long idUsuario) {
        buscarUsuario(idUsuario);
        List<Perfil> perfiles = perfilRepository.findByUsuarioIdUsuario(idUsuario);
        return perfiles.stream()
                .map(AdminPerfilMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PerfilResponse> listarPerfilesPropios(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return perfilRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(PerfilMapper::toResponse)
                .toList();
    }

    private Usuario buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioAdminNoEncontradoException("Usuario no encontrado."));
    }
}
