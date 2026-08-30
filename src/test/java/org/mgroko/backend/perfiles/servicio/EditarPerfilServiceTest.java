package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaPerfilId;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.ImagenNoEncontradaException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.ImagenRepository;
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
class EditarPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private CaracteristicaPerfilHelper caracteristicaPerfilHelper;

    @InjectMocks
    private EditarPerfilService editarPerfilService;

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

    private EditarPerfilRequest requestValido() {
        return new EditarPerfilRequest(
                "Luna Nova", "Modelo profesional renovada.", null,
                List.of(new CaracteristicaPerfilRequest(11L, "176", null)));
    }

    @Test
    void editar_datosValidos_actualizaPerfil() {
        Perfil perfil = perfilModelo();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(caracteristicaPerfilHelper.construir(eq(perfil), eq(perfil.getProfesion()), any())).thenReturn(List.of());
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerfilResponse response = editarPerfilService.editar(1L, 10L, requestValido());

        assertEquals("Luna Nova", response.nombreArtistico());
        assertEquals("Modelo profesional renovada.", response.biografia());
        assertEquals("modelo", response.profesion());

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(captor.capture());
        Perfil guardado = captor.getValue();
        assertEquals("Luna Nova", guardado.getNombreArtistico());
        assertEquals("Modelo profesional renovada.", guardado.getBiografia());
        assertEquals(2L, guardado.getProfesion().getIdProfesion());
    }

    @Test
    void editar_conImagen_asignaImagen() {
        Imagen imagen = Imagen.builder().idImagen(7L).url("http://img").build();
        Perfil perfil = perfilModelo();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(imagenRepository.findById(7L)).thenReturn(Optional.of(imagen));
        when(caracteristicaPerfilHelper.construir(eq(perfil), eq(perfil.getProfesion()), any())).thenReturn(List.of());
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EditarPerfilRequest request = new EditarPerfilRequest(
                "Luna Nova", "Modelo profesional renovada.", 7L, List.of());

        editarPerfilService.editar(1L, 10L, request);

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getImagen().getIdImagen());
    }

    @Test
    void editar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> editarPerfilService.editar(999L, 10L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void editar_usuarioNoActivo_lanzaExcepcion() {
        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDeshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> editarPerfilService.editar(1L, 10L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void editar_perfilNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> editarPerfilService.editar(1L, 10L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void editar_perfilEnBaja_lanzaExcepcion() {
        Perfil perfilBaja = perfilModelo();
        perfilBaja.setEstado(EstadoPerfil.Baja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilBaja));

        assertThrows(PerfilEnBajaException.class,
                () -> editarPerfilService.editar(1L, 10L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void editar_imagenInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfilModelo()));
        when(imagenRepository.findById(99L)).thenReturn(Optional.empty());

        EditarPerfilRequest request = new EditarPerfilRequest(
                "Luna Nova", "Modelo profesional renovada.", 99L, List.of());

        assertThrows(ImagenNoEncontradaException.class,
                () -> editarPerfilService.editar(1L, 10L, request));
    }

    @Test
    void editar_delegaConstruccionDeCaracteristicas() {
        Perfil perfil = perfilModelo();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(caracteristicaPerfilHelper.construir(eq(perfil), eq(perfil.getProfesion()), any()))
                .thenReturn(List.of());
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        editarPerfilService.editar(1L, 10L, requestValido());

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getCaracteristicas().size());
    }
}