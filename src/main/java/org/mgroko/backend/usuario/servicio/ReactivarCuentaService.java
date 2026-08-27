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
import org.mgroko.backend.usuario.dto.ReactivarCuentaResponse;
import org.mgroko.backend.usuario.exception.SolicitudBajaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactivarCuentaService {

    private static final int DIAS_PARA_ELIMINACION = 30;

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public ReactivarCuentaService(UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public ReactivarCuentaResponse reactivarCuenta(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (usuario.getEstado() != EstadoUsuario.PendienteBaja) {
            throw new SolicitudBajaException("No hay una solicitud de baja activa para esta cuenta.");
        }

        if (usuario.getFechaSolicitudBaja() != null
                && usuario.getFechaSolicitudBaja().plusDays(DIAS_PARA_ELIMINACION).isBefore(LocalDateTime.now())) {
            throw new SolicitudBajaException(
                    "El plazo de " + DIAS_PARA_ELIMINACION
                            + " días para recuperar la cuenta ha expirado.");
        }

        usuario.setEstado(EstadoUsuario.Activo);
        usuario.setFechaSolicitudBaja(null);
        usuarioRepository.save(usuario);

        List<Perfil> perfiles = perfilRepository.findByUsuarioIdUsuario(idUsuario);
        for (Perfil perfil : perfiles) {
            perfil.setEstado(EstadoPerfil.Activo);
            perfil.setFechaSolicitudBaja(null);
        }
        perfilRepository.saveAll(perfiles);

        return new ReactivarCuentaResponse("Tu cuenta ha sido reactivada exitosamente.");
    }
}
