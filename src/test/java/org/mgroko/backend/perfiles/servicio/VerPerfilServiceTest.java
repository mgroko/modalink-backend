package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.PerfilDetalleResponse;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class VerPerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private VerPerfilService verPerfilService;

    private Usuario usuarioAutenticado;
    private Usuario usuarioDuenio;
    private Profesion profesion;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = Usuario.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .apellido("Perez")
                .correo("juan@test.com")
                .estado(EstadoUsuario.Activo)
                .build();

        usuarioDuenio = Usuario.builder()
                .idUsuario(2L)
                .nombre("Maria")
                .apellido("Gomez")
                .correo("maria@test.com")
                .estado(EstadoUsuario.Activo)
                .genero(Genero.builder().idGenero(1L).codigo("FEM").build())
                .ubicacion(Ubicacion.builder().idUbicacion(1L).localidad("Rosario").provincia("Santa Fe").build())
                .build();

        profesion = Profesion.builder()
                .idProfesion(10L)
                .nombre("Modelo")
                .build();

        perfil = Perfil.builder()
                .idPerfil(100L)
                .nombreArtistico("Mary Model")
                .biografia("Experiencia en pasarela.")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDuenio)
                .profesion(profesion)
                .caracteristicas(new HashSet<>())
                .habilidades(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Debe retornar detalle de perfil ajeno cuando el perfil y el dueño están activos")
    void obtenerDetalle_perfilAjenoActivo_retornaDetalleConEsPropietarioFalse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(perfil));

        PerfilDetalleResponse response = verPerfilService.obtenerDetalle(100L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.idPerfil()).isEqualTo(100L);
        assertThat(response.nombreArtistico()).isEqualTo("Mary Model");
        assertThat(response.profesion()).isEqualTo("Modelo");
        assertThat(response.idUsuario()).isEqualTo(2L);
        assertThat(response.nombreUsuario()).isEqualTo("Maria");
        assertThat(response.apellidoUsuario()).isEqualTo("Gomez");
        assertThat(response.localidad()).isEqualTo("Rosario");
        assertThat(response.provincia()).isEqualTo("Santa Fe");
        assertThat(response.genero()).isEqualTo("FEM");
        assertThat(response.esPropietario()).isFalse();
    }

    @Test
    @DisplayName("Debe permitir ver perfil propio incluso en estado PendienteBaja")
    void obtenerDetalle_perfilPropioPendienteBaja_retornaDetalleConEsPropietarioTrue() {
        perfil.setUsuario(usuarioAutenticado);
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        perfil.setFechaSolicitudBaja(LocalDateTime.now().plusDays(30));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(perfil));

        PerfilDetalleResponse response = verPerfilService.obtenerDetalle(100L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.idPerfil()).isEqualTo(100L);
        assertThat(response.estado()).isEqualTo("PendienteBaja");
        assertThat(response.fechaSolicitudBaja()).isNotNull();
        assertThat(response.esPropietario()).isTrue();
    }

    @Test
    @DisplayName("Debe lanzar 404 si el perfil ajeno está en PendienteBaja")
    void obtenerDetalle_perfilAjenoPendienteBaja_lanzaPerfilNoEncontradoException() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> verPerfilService.obtenerDetalle(100L, 1L))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("Perfil no encontrado.");
    }

    @Test
    @DisplayName("Debe lanzar 404 si el dueño del perfil ajeno está en PendienteBaja o Baja")
    void obtenerDetalle_duenioNoActivo_lanzaPerfilNoEncontradoException() {
        usuarioDuenio.setEstado(EstadoUsuario.PendienteBaja);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> verPerfilService.obtenerDetalle(100L, 1L))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("Perfil no encontrado.");
    }

    @Test
    @DisplayName("Debe lanzar 404 si el perfil está en Baja definitiva incluso para el dueño")
    void obtenerDetalle_perfilEnBajaDefinitiva_lanzaPerfilNoEncontradoException() {
        perfil.setUsuario(usuarioAutenticado);
        perfil.setEstado(EstadoPerfil.Baja);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> verPerfilService.obtenerDetalle(100L, 1L))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("Perfil no encontrado.");
    }

    @Test
    @DisplayName("Debe lanzar 404 si el perfil no existe")
    void obtenerDetalle_perfilInexistente_lanzaPerfilNoEncontradoException() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));
        when(perfilRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verPerfilService.obtenerDetalle(999L, 1L))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("Perfil no encontrado.");
    }

    @Test
    @DisplayName("Debe lanzar 401/UsuarioNoEncontradoException si el usuario solicitante no existe o no tiene acceso")
    void obtenerDetalle_usuarioNoPermiteAcceso_lanzaUsuarioNoEncontradoException() {
        usuarioAutenticado.setEstado(EstadoUsuario.Baja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAutenticado));

        assertThatThrownBy(() -> verPerfilService.obtenerDetalle(100L, 1L))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }
}
