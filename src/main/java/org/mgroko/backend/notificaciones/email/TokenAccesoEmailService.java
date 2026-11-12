package org.mgroko.backend.notificaciones.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auth.exception.TokenEmailException;
import org.mgroko.backend.modelo.NotificacionEntrega;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.TokenAccesoEmail;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.NotificacionEntregaRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.TokenAccesoEmailRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera y valida tokens de un solo uso para auto-login desde un email.
 *
 * <p>Solo se persiste el hash SHA-256 del token. El valor crudo viaja en el
 * link del correo y se intercambia por una sesión en
 * {@code POST /auth/email/validar}.</p>
 */
@Service
public class TokenAccesoEmailService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private final TokenAccesoEmailRepository tokenRepository;
    private final NotificacionEntregaRepository entregaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final Clock reloj;

    public TokenAccesoEmailService(
            TokenAccesoEmailRepository tokenRepository,
            NotificacionEntregaRepository entregaRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            Clock reloj) {
        this.tokenRepository = tokenRepository;
        this.entregaRepository = entregaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.reloj = reloj;
    }

    /**
     * Genera un token nuevo para una entrega. Si ya existía uno, lo reemplaza:
     * el token anterior deja de ser válido.
     */
    @Transactional
    public String generarToken(
            Long idEntrega,
            Long idUsuario,
            Long idPerfilContexto) {

        LocalDateTime ahora = LocalDateTime.now(reloj);
        int ttlMinutos = configuracionSistemaService.obtenerTtlTokenEmailMinutos();

        String crudo = generarValorCrudo();
        String hash = hash(crudo);

        TokenAccesoEmail token = tokenRepository.findByEntrega_IdEntrega(idEntrega)
                .orElseGet(() -> TokenAccesoEmail.builder()
                        .entrega(entregaRepository.getReferenceById(idEntrega))
                        .usuario(usuarioRepository.getReferenceById(idUsuario))
                        .build());

        if (idPerfilContexto != null) {
            Perfil perfil = perfilRepository.getReferenceById(idPerfilContexto);
            token.setPerfilContexto(perfil);
        } else {
            token.setPerfilContexto(null);
        }

        token.setTokenHash(hash);
        token.setCreadoEn(ahora);
        token.setExpiraEn(ahora.plusMinutes(ttlMinutos));
        token.setUsadoEn(null);
        tokenRepository.save(token);

        return crudo;
    }

    @Transactional
    public TokenValidado validarYConsumir(String tokenCrudo) {
        if (tokenCrudo == null || tokenCrudo.isBlank()) {
            throw TokenEmailException.requerido();
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        TokenAccesoEmail token = tokenRepository
                .findByTokenHashForUpdate(hash(tokenCrudo))
                .orElseThrow(TokenEmailException::invalido);

        if (token.getUsadoEn() != null) {
            throw TokenEmailException.usado();
        }
        if (token.getExpiraEn() == null || token.getExpiraEn().isBefore(ahora)) {
            throw TokenEmailException.expirado();
        }

        Long idUsuario = token.getUsuario().getIdUsuario();
        Usuario usuario = usuarioRepository.findByIdConRolYPermisos(idUsuario)
                .orElseThrow(TokenEmailException::invalido);

        if (usuario.getEstado() == EstadoUsuario.BAJA
                || usuario.getEstado() == EstadoUsuario.DESHABILITADO) {
            throw TokenEmailException.usuarioNoActivo();
        }

        token.setUsadoEn(ahora);
        tokenRepository.save(token);

        Long idPerfilContexto = token.getPerfilContexto() != null
                ? token.getPerfilContexto().getIdPerfil()
                : null;

        return new TokenValidado(usuario.getIdUsuario(), idPerfilContexto);
    }

    @Transactional
    public int limpiarVencidos() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int dias = configuracionSistemaService.obtenerDiasLimpiezaTokenEmail();
        return tokenRepository.eliminarAnteriores(ahora.minusDays(dias));
    }

    private String generarValorCrudo() {
        byte[] bytes = new byte[BYTES_TOKEN];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    public record TokenValidado(Long idUsuario, Long idPerfilContexto) {
    }
}
