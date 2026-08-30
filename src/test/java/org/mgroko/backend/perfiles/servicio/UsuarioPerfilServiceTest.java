package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioPerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioPerfilService usuarioPerfilService;

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Perfil perfilModelo() {
        return Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.Activo)
                .profesion(Profesion.builder().idProfesion(2L).nombre("modelo").build())
                .build();
    }

    @Test
    void listarPerfilesPropios_retornaPerfilesDelUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(perfilModelo()));

        List<PerfilResponse> response = usuarioPerfilService.listarPerfilesPropios(1L);

        assertEquals(1, response.size());
        assertEquals("Luna", response.get(0).nombreArtistico());
        assertEquals("modelo", response.get(0).profesion());
        assertEquals("Activo", response.get(0).estado());
    }

    @Test
    void listarPerfilesPropios_sinPerfiles_retornaListaVacia() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());

        List<PerfilResponse> response = usuarioPerfilService.listarPerfilesPropios(1L);

        assertTrue(response.isEmpty());
    }

    @Test
    void listarPerfilesPropios_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> usuarioPerfilService.listarPerfilesPropios(999L));

        verify(perfilRepository, never()).findByUsuarioIdUsuario(999L);
    }

    @Test
    void listarPerfilesPropios_usuarioNoActivo_lanzaExcepcion() {
        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDeshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> usuarioPerfilService.listarPerfilesPropios(1L));

        verify(perfilRepository, never()).findByUsuarioIdUsuario(1L);
    }

    @Test
    void obtenerPerfilPropio_retornaPerfilDelUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilModelo()));

        PerfilResponse response = usuarioPerfilService.obtenerPerfilPropio(1L, 10L);

        assertEquals("Luna", response.nombreArtistico());
        assertEquals("modelo", response.profesion());
        assertEquals("Activo", response.estado());
    }

    @Test
    void obtenerPerfilPropio_perfilDeOtroUsuario_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> usuarioPerfilService.obtenerPerfilPropio(1L, 10L));
    }
}