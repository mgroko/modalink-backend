package org.mgroko.backend.notificaciones.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenAccesoEmailServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 11, 12, 0);

    @Mock
    private TokenAccesoEmailRepository tokenRepository;
    @Mock
    private NotificacionEntregaRepository entregaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    private TokenAccesoEmailService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);
        service = new TokenAccesoEmailService(
                tokenRepository,
                entregaRepository,
                usuarioRepository,
                perfilRepository,
                configuracionSistemaService,
                reloj);
    }

    @Test
    void generarToken_persisteSoloHashYExpiraEn60Minutos() {
        when(configuracionSistemaService.obtenerTtlTokenEmailMinutos()).thenReturn(60);
        when(tokenRepository.findByEntrega_IdEntrega(10L)).thenReturn(Optional.empty());
        when(entregaRepository.getReferenceById(10L))
                .thenReturn(NotificacionEntrega.builder().idEntrega(10L).build());
        when(usuarioRepository.getReferenceById(20L))
                .thenReturn(Usuario.builder().idUsuario(20L).build());
        when(perfilRepository.getReferenceById(30L))
                .thenReturn(Perfil.builder().idPerfil(30L).build());
        when(tokenRepository.save(any(TokenAccesoEmail.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String crudo = service.generarToken(10L, 20L, 30L);

        assertThat(crudo).isNotBlank();
        ArgumentCaptor<TokenAccesoEmail> captor = ArgumentCaptor.forClass(TokenAccesoEmail.class);
        verify(tokenRepository).save(captor.capture());
        TokenAccesoEmail token = captor.getValue();
        assertThat(token.getTokenHash()).hasSize(64).isNotEqualTo(crudo);
        assertThat(token.getExpiraEn()).isEqualTo(AHORA.plusMinutes(60));
        assertThat(token.getPerfilContexto().getIdPerfil()).isEqualTo(30L);
        assertThat(token.getUsadoEn()).isNull();
    }

    @Test
    void validarTokenValido_marcaUsadoYDevuelvePerfil() {
        TokenAccesoEmail token = tokenValido();
        Usuario usuario = Usuario.builder()
                .idUsuario(20L)
                .estado(EstadoUsuario.ACTIVO)
                .build();
        when(tokenRepository.findByTokenHashForUpdate(hash("token")))
                .thenReturn(Optional.of(token));
        when(usuarioRepository.findByIdConRolYPermisos(20L))
                .thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any(TokenAccesoEmail.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TokenAccesoEmailService.TokenValidado validado = service.validarYConsumir("token");

        assertThat(validado.idUsuario()).isEqualTo(20L);
        assertThat(validado.idPerfilContexto()).isEqualTo(30L);
        assertThat(token.getUsadoEn()).isEqualTo(AHORA);
    }

    @Test
    void tokenUsado_falla() {
        TokenAccesoEmail token = tokenValido();
        token.setUsadoEn(AHORA.minusMinutes(1));
        when(tokenRepository.findByTokenHashForUpdate(hash("token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarYConsumir("token"))
                .isInstanceOfSatisfying(TokenEmailException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("TOKEN_USADO"));
    }

    @Test
    void tokenVencido_falla() {
        TokenAccesoEmail token = tokenValido();
        token.setExpiraEn(AHORA.minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(hash("token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarYConsumir("token"))
                .isInstanceOfSatisfying(TokenEmailException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("TOKEN_EXPIRADO"));
    }

    @Test
    void tokenVacio_falla() {
        assertThatThrownBy(() -> service.validarYConsumir(" "))
                .isInstanceOfSatisfying(TokenEmailException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("TOKEN_REQUERIDO"));
    }

    @Test
    void usuarioDeshabilitado_falla() {
        TokenAccesoEmail token = tokenValido();
        Usuario usuario = Usuario.builder()
                .idUsuario(20L)
                .estado(EstadoUsuario.DESHABILITADO)
                .build();
        when(tokenRepository.findByTokenHashForUpdate(hash("token")))
                .thenReturn(Optional.of(token));
        when(usuarioRepository.findByIdConRolYPermisos(20L))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarYConsumir("token"))
                .isInstanceOfSatisfying(TokenEmailException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("USUARIO_NO_ACTIVO"));
    }

    private TokenAccesoEmail tokenValido() {
        return TokenAccesoEmail.builder()
                .idToken(1L)
                .usuario(Usuario.builder().idUsuario(20L).build())
                .perfilContexto(Perfil.builder().idPerfil(30L).build())
                .tokenHash(hash("token"))
                .creadoEn(AHORA)
                .expiraEn(AHORA.plusMinutes(60))
                .build();
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
