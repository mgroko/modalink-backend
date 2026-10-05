package org.mgroko.backend.usuario.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
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

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public SolicitudBajaService(UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Transactional
    public SolicitudBajaResponse solicitarBaja(Long idUsuario) {
        int diasBaja = configuracionSistemaService.obtenerDiasBaja();

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

        LocalDateTime fechaLimite = ahora.plusDays(diasBaja);

        return new SolicitudBajaResponse(
                "Solicitud de baja registrada. Tienes " + diasBaja
                        + " días para recuperar tus datos.",
                fechaLimite);
    }
}
