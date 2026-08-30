package org.mgroko.backend.perfiles.servicio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.EliminarPerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EliminarPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private EliminarPerfilService eliminarPerfilService;

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
    void eliminar_perfilActivo_registraCuentaRegresiva() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilModelo()));
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EliminarPerfilResponse response = eliminarPerfilService.eliminar(1L, 10L);

        assertNotNull(response.mensaje());
        assertEquals(java.time.LocalDate.now().plusDays(30), response.fechaLimite().toLocalDate());

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(captor.capture());
        assertEquals(EstadoPerfil.PendienteBaja, captor.getValue().getEstado());
        assertNotNull(captor.getValue().getFechaSolicitudBaja());
    }

    @Test
    void eliminar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> eliminarPerfilService.eliminar(999L, 10L));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void eliminar_perfilNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> eliminarPerfilService.eliminar(1L, 10L));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void eliminar_perfilEnBaja_lanzaExcepcion() {
        Perfil perfilBaja = perfilModelo();
        perfilBaja.setEstado(EstadoPerfil.Baja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilBaja));

        assertThrows(PerfilEnBajaException.class,
                () -> eliminarPerfilService.eliminar(1L, 10L));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void eliminar_perfilPendienteBaja_lanzaExcepcion() {
        Perfil perfilPendiente = perfilModelo();
        perfilPendiente.setEstado(EstadoPerfil.PendienteBaja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilPendiente));

        assertThrows(PerfilEnBajaException.class,
                () -> eliminarPerfilService.eliminar(1L, 10L));

        verify(perfilRepository, never()).save(any());
    }
}