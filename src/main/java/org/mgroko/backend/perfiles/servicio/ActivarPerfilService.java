package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.auth.AuthService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivarPerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final JwtService jwtService;

    public ActivarPerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
            AuthService authService, JwtService jwtService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public ActivarPerfilResultado activar(Long idUsuario, Long idPerfil) {
        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (!perfil.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new PerfilNoEncontradoException();
        }

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("No se puede activar un perfil que está en proceso de baja o dado de baja.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();

        String token = jwtService.generarToken(
                idUsuario.toString(),
                authService.construirClaims(usuario, perfil)
        );

        return new ActivarPerfilResultado(token, PerfilMapper.toResponse(perfil));
    }

    public record ActivarPerfilResultado(String token, PerfilResponse perfil) {}
}