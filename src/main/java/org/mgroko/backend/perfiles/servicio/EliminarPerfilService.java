package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.EliminarPerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarPerfilService {

    private static final int DIAS_PARA_ELIMINACION = 30;

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public EliminarPerfilService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public EliminarPerfilResponse eliminar(Long idUsuario, Long idPerfil) {
        buscarUsuarioActivo(idUsuario);

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfil, idUsuario)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        if (perfil.getEstado() == EstadoPerfil.Baja) {
            throw new PerfilEnBajaException("El perfil ya fue dado de baja.");
        }

        if (perfil.getEstado() == EstadoPerfil.PendienteBaja) {
            throw new PerfilEnBajaException("Ya existe una solicitud de baja activa para este perfil.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        perfil.setFechaSolicitudBaja(ahora);
        perfilRepository.save(perfil);

        // TODO: Ocultar publicaciones independientes del perfil (tabla publicacion aún no implementada).

        LocalDateTime fechaLimite = ahora.plusDays(DIAS_PARA_ELIMINACION);

        return new EliminarPerfilResponse(
                "Solicitud de baja registrada. Tienes " + DIAS_PARA_ELIMINACION
                        + " días para activar el perfil.",
                fechaLimite);
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