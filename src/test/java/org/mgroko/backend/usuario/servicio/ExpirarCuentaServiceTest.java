package org.mgroko.backend.usuario.servicio;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirarCuentaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private ExpirarCuentaService expirarCuentaService;

    private Usuario usuarioVencido() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .estado(EstadoUsuario.PendienteBaja)
                .fechaSolicitudBaja(LocalDateTime.now().minusDays(31))
                .build();
    }

    private Perfil perfil(Usuario usuario) {
        return Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.PendienteBaja)
                .usuario(usuario)
                .build();
    }

    @Test
    void expirarVencidos_cuentaYPerfilesPasanABaja() {
        Usuario usuario = usuarioVencido();
        Perfil perfil = perfil(usuario);

        when(usuarioRepository.findByEstadoAndFechaSolicitudBajaBefore(eq(EstadoUsuario.PendienteBaja), any()))
                .thenReturn(List.of(usuario));
        when(perfilRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(perfil));

        int cantidad = expirarCuentaService.expirarVencidos();

        assertEquals(1, cantidad);
        assertEquals(EstadoUsuario.Baja, usuario.getEstado());

        ArgumentCaptor<List<Perfil>> captor = ArgumentCaptor.forClass(List.class);
        verify(perfilRepository).saveAll(captor.capture());
        assertEquals(EstadoPerfil.Baja, captor.getValue().get(0).getEstado());

        ArgumentCaptor<List<Usuario>> captorUsuario = ArgumentCaptor.forClass(List.class);
        verify(usuarioRepository).saveAll(captorUsuario.capture());
        assertEquals(EstadoUsuario.Baja, captorUsuario.getValue().get(0).getEstado());
    }

    @Test
    void expirarVencidos_perfilYaEnBaja_noLoReintenta() {
        Usuario usuario = usuarioVencido();
        Perfil perfil = perfil(usuario);
        perfil.setEstado(EstadoPerfil.Baja);

        when(usuarioRepository.findByEstadoAndFechaSolicitudBajaBefore(eq(EstadoUsuario.PendienteBaja), any()))
                .thenReturn(List.of(usuario));
        when(perfilRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(perfil));

        expirarCuentaService.expirarVencidos();

        ArgumentCaptor<List<Perfil>> captor = ArgumentCaptor.forClass(List.class);
        verify(perfilRepository).saveAll(captor.capture());
        assertEquals(EstadoPerfil.Baja, captor.getValue().get(0).getEstado());
    }

    @Test
    void expirarVencidos_sinVencidos_noGuarda() {
        when(usuarioRepository.findByEstadoAndFechaSolicitudBajaBefore(eq(EstadoUsuario.PendienteBaja), any()))
                .thenReturn(List.of());

        int cantidad = expirarCuentaService.expirarVencidos();

        assertEquals(0, cantidad);
        verify(usuarioRepository, never()).saveAll(any());
        verify(perfilRepository, never()).saveAll(any());
    }
}