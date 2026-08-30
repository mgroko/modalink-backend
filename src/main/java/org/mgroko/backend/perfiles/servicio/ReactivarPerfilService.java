package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactivarPerfilService {

    private static final int DIAS_PARA_ELIMINACION = 30;

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public ReactivarPerfilService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public Perfil reactivar(Long idUsuario, Long idPerfil) {
        buscarUsuarioActivo(idUsuario);

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        if (perfil.getEstado() != EstadoPerfil.PendienteBaja) {
            throw new PerfilEnBajaException("No hay una solicitud de baja activa para este perfil.");
        }

        if (perfil.getFechaSolicitudBaja() != null
                && perfil.getFechaSolicitudBaja().plusDays(DIAS_PARA_ELIMINACION).isBefore(LocalDateTime.now())) {
            throw new PerfilEnBajaException(
                    "El plazo de " + DIAS_PARA_ELIMINACION + " días para activar el perfil ha expirado.");
        }

        perfil.setEstado(EstadoPerfil.Activo);
        perfil.setFechaSolicitudBaja(null);
        return perfilRepository.save(perfil);
    }

    private Usuario buscarUsuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return usuario;
    }
}