package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactivarPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private ReactivarPerfilService reactivarPerfilService;

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Perfil perfilPendiente() {
        return Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .biografia("Modelo profesional.")
                .estado(EstadoPerfil.PendienteBaja)
                .fechaSolicitudBaja(LocalDateTime.now().minusDays(1))
                .profesion(Profesion.builder().idProfesion(2L).nombre("modelo").build())
                .build();
    }

    @Test
    void reactivar_dentroDePlazo_vuelveActivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilPendiente()));
        when(perfilRepository.save(org.mockito.ArgumentMatchers.any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Perfil perfil = reactivarPerfilService.reactivar(1L, 10L);

        assertEquals(EstadoPerfil.Activo, perfil.getEstado());
        assertNull(perfil.getFechaSolicitudBaja());
    }

    @Test
    void reactivar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> reactivarPerfilService.reactivar(999L, 10L));
    }

    @Test
    void reactivar_perfilNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> reactivarPerfilService.reactivar(1L, 10L));
    }

    @Test
    void reactivar_perfilNoPendiente_lanzaExcepcion() {
        Perfil activo = perfilPendiente();
        activo.setEstado(EstadoPerfil.Activo);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(activo));

        assertThrows(PerfilEnBajaException.class,
                () -> reactivarPerfilService.reactivar(1L, 10L));
    }

    @Test
    void reactivar_plazoExpirado_lanzaExcepcion() {
        Perfil vencido = perfilPendiente();
        vencido.setFechaSolicitudBaja(LocalDateTime.now().minusDays(31));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(vencido));

        assertThrows(PerfilEnBajaException.class,
                () -> reactivarPerfilService.reactivar(1L, 10L));
    }
}