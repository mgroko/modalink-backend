package org.mgroko.backend.perfiles.servicio;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.AuthService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.JwtService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivarPerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private ActivarPerfilService activarPerfilService;

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).build();
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, EstadoPerfil estado) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(estado)
                .profesion(Profesion.builder().idProfesion(2L).nombre("modelo").build())
                .usuario(usuario(idUsuario))
                .build();
    }

    @Test
    void activar_perfilPropioActivo_devuelveNuevoTokenYPerfil() {
        Perfil perfil = perfil(10L, 1L, EstadoPerfil.Activo);

        when(perfilRepository.findById(10L)).thenReturn(Optional.of(perfil));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(authService.construirClaims(any(), any())).thenReturn(Map.of("idPerfilActivo", 10L));
        when(jwtService.generarToken(anyString(), anyMap())).thenReturn("token-nuevo");

        ActivarPerfilService.ActivarPerfilResultado resultado = activarPerfilService.activar(1L, 10L);

        assertEquals("token-nuevo", resultado.token());
        assertEquals(10L, resultado.perfil().idPerfil());
        assertEquals("Luna", resultado.perfil().nombreArtistico());

        verify(authService).construirClaims(any(), any());
        verify(jwtService).generarToken(anyString(), anyMap());
    }

    @Test
    void activar_perfilDeOtroUsuario_lanzaPerfilNoEncontrado() {
        Perfil perfil = perfil(10L, 2L, EstadoPerfil.Activo);
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(perfil));

        assertThrows(PerfilNoEncontradoException.class, () -> activarPerfilService.activar(1L, 10L));
    }

    @Test
    void activar_perfilNoActivo_lanzaPerfilEnBaja() {
        Perfil perfil = perfil(10L, 1L, EstadoPerfil.PendienteBaja);
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(perfil));

        assertThrows(PerfilEnBajaException.class, () -> activarPerfilService.activar(1L, 10L));
    }

    @Test
    void activar_perfilInexistente_lanzaPerfilNoEncontrado() {
        when(perfilRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class, () -> activarPerfilService.activar(1L, 999L));
    }
}