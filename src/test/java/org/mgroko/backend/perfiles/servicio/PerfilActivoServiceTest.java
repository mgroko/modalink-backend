package org.mgroko.backend.perfiles.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.exception.AccesoDenegadoPorRolException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerfilActivoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    private PerfilActivoService service;

    private Usuario usuario;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        service = new PerfilActivoService(usuarioRepository, perfilRepository);

        usuario = Usuario.builder()
                .idUsuario(ID_USUARIO)
                .nombre("Ana")
                .apellido("Pérez")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(RolGlobal.builder()
                        .idRolGlobal(1L)
                        .codigo("USUARIO")
                        .nombre("Usuario")
                        .build())
                .build();

        perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Ana Moda")
                .biografia("Perfil activo")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .build();
    }

    @Test
    void resolver_perfilActivoPropio_retornaPerfil() {
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil));

        Perfil resultado = service.resolver(ID_USUARIO, ID_PERFIL);

        assertThat(resultado).isSameAs(perfil);
    }

    @Test
    void resolver_usuarioInexistente_lanzaUsuarioNoEncontrado() {
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void resolver_usuarioSinAcceso_lanzaUsuarioNoEncontrado() {
        usuario.setEstado(EstadoUsuario.Baja);
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void resolver_administrador_lanzaAccesoDenegado() {
        usuario.setRolGlobal(RolGlobal.builder()
                .idRolGlobal(2L)
                .codigo("ADMINISTRADOR")
                .nombre("Administrador")
                .build());
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(AccesoDenegadoPorRolException.class);
    }

    @Test
    void resolver_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, null))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void resolver_perfilDeOtroUsuario_lanzaPerfilNoEncontrado() {
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void resolver_perfilNoActivo_lanzaPerfilEnBaja() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void resolver_usuarioDelPerfilInactivo_lanzaPerfilEnBaja() {
        usuario.setEstado(EstadoUsuario.PendienteBaja);
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> service.resolver(ID_USUARIO, ID_PERFIL))
                .isInstanceOf(PerfilEnBajaException.class);
    }
}
