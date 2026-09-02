package org.mgroko.backend.usuario.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.usuario.dto.SolicitudBajaResponse;
import org.mgroko.backend.usuario.exception.SolicitudBajaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudBajaService {

    private static final int DIAS_PARA_ELIMINACION = 30; //TODO parametrizable desde panel de admin? 

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public SolicitudBajaService(UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public SolicitudBajaResponse solicitarBaja(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (usuario.getEstado() == EstadoUsuario.PendienteBaja) {
            throw new SolicitudBajaException("Ya existe una solicitud de baja activa.");
        }

        if (usuario.getEstado() == EstadoUsuario.Baja) {
            throw new SolicitudBajaException("La cuenta ya fue dada de baja.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        usuario.setEstado(EstadoUsuario.PendienteBaja);
        usuario.setFechaSolicitudBaja(ahora);
        usuarioRepository.save(usuario);

        List<Perfil> perfiles = perfilRepository.findByUsuarioIdUsuario(idUsuario);
        for (Perfil perfil : perfiles) {
            perfil.setEstado(EstadoPerfil.PendienteBaja);
            perfil.setFechaSolicitudBaja(ahora);
        }
        perfilRepository.saveAll(perfiles);

        // TODO: Ocultar publicaciones independientes del usuario

        LocalDateTime fechaLimite = ahora.plusDays(DIAS_PARA_ELIMINACION);

        return new SolicitudBajaResponse(
                "Solicitud de baja registrada. Tienes " + DIAS_PARA_ELIMINACION
                        + " días para recuperar tus datos.",
                fechaLimite);
    }
}
