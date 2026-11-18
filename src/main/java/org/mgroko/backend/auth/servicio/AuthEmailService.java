package org.mgroko.backend.auth.servicio;

import org.mgroko.backend.auth.exception.TokenEmailException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.notificaciones.email.TokenAccesoEmailService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intercambia un token de email por una sesión JWT.
 */
@Service
public class AuthEmailService {

    private final TokenAccesoEmailService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final AuthService authService;

    public AuthEmailService(
            TokenAccesoEmailService tokenService,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            AuthService authService) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.authService = authService;
    }

    @Transactional
    public AuthService.LoginResultado loginConToken(String tokenCrudo) {
        TokenAccesoEmailService.TokenValidado validado =
                tokenService.validarYConsumir(tokenCrudo);

        Usuario usuario = usuarioRepository
                .findByIdConRolYPermisos(validado.idUsuario())
                .orElseThrow(TokenEmailException::invalido);

        Perfil perfilContexto = null;
        if (validado.idPerfilContexto() != null) {
            perfilContexto = perfilRepository
                    .findByIdPerfilAndUsuarioIdUsuario(
                            validado.idPerfilContexto(),
                            validado.idUsuario())
                    .filter(perfil -> perfil.getEstado() == EstadoPerfil.ACTIVO)
                    .orElse(null);
        }

        return perfilContexto != null
                ? authService.construirLoginResultado(usuario, perfilContexto)
                : authService.construirLoginResultado(usuario);
    }
}
