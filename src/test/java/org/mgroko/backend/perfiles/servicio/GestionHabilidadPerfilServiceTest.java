package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.exception.HabilidadNoAsociadaException;
import org.mgroko.backend.perfiles.exception.HabilidadYaAsociadaException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestionHabilidadPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private HabilidadRepository habilidadRepository;

    @InjectMocks
    private GestionHabilidadPerfilService service;

    private Usuario usuario() {
        return Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Perfil perfil(EstadoPerfil estado) {
        return Perfil.builder()
                .idPerfil(5L)
                .estado(estado)
                .usuario(usuario())
                .build();
    }

    private Habilidad habilidad(Long id, String nombre) {
        return Habilidad.builder()
                .idHabilidad(id)
                .codigo("CODIGO_" + id)
                .nombre(nombre)
                .build();
    }

    private void usuarioActivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario()));
    }

    private void perfilPropio(Perfil perfil) {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(5L, 1L))
                .thenReturn(Optional.of(perfil));
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_ordenaPorNombre() {
        Perfil perfil = perfil(EstadoPerfil.Activo);
        perfil.getHabilidades().add(habilidad(2L, "Zeta"));
        perfil.getHabilidades().add(habilidad(1L, "alfa"));
        usuarioActivo();
        perfilPropio(perfil);

        List<HabilidadResponse> resultado = service.listar(1L, 5L);

        assertEquals(2, resultado.size());
        assertEquals("alfa", resultado.get(0).nombre());
        assertEquals("Zeta", resultado.get(1).nombre());
    }

    @Test
    void listar_perfilAjeno_lanza404() {
        usuarioActivo();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(5L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(PerfilNoEncontradoException.class,
                () -> service.listar(1L, 5L));
    }

    @Test
    void listar_usuarioSinAcceso_lanza404() {
        Usuario usuario = usuario();
        usuario.setEstado(EstadoUsuario.Baja);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> service.listar(1L, 5L));
    }

    // ------------------------------------------------------------ agregar

    @Test
    void agregar_exitoso_asociaHabilidad() {
        Perfil perfil = perfil(EstadoPerfil.Activo);
        Habilidad habilidad = habilidad(3L, "Retoque");
        usuarioActivo();
        perfilPropio(perfil);
        when(habilidadRepository.findById(3L)).thenReturn(Optional.of(habilidad));

        HabilidadResponse response = service.agregar(1L, 5L, 3L);

        assertEquals(3L, response.idHabilidad());
        assertTrue(perfil.getHabilidades().contains(habilidad));
        verify(perfilRepository).save(perfil);
    }

    @Test
    void agregar_yaAsociada_lanza409() {
        Perfil perfil = perfil(EstadoPerfil.Activo);
        Habilidad habilidad = habilidad(3L, "Retoque");
        perfil.getHabilidades().add(habilidad);
        usuarioActivo();
        perfilPropio(perfil);
        when(habilidadRepository.findById(3L)).thenReturn(Optional.of(habilidad));

        assertThrows(HabilidadYaAsociadaException.class,
                () -> service.agregar(1L, 5L, 3L));

        verify(perfilRepository, never()).save(any(Perfil.class));
    }

    @Test
    void agregar_habilidadInexistente_lanza404() {
        usuarioActivo();
        perfilPropio(perfil(EstadoPerfil.Activo));
        when(habilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(HabilidadNoEncontradaException.class,
                () -> service.agregar(1L, 5L, 99L));
    }

    @Test
    void agregar_perfilEnBaja_lanza409() {
        usuarioActivo();
        perfilPropio(perfil(EstadoPerfil.Baja));

        assertThrows(PerfilEnBajaException.class,
                () -> service.agregar(1L, 5L, 3L));

        verify(habilidadRepository, never()).findById(any(Long.class));
    }

    // -------------------------------------------------------------- quitar

    @Test
    void quitar_exitoso_desasociaHabilidad() {
        Perfil perfil = perfil(EstadoPerfil.Activo);
        Habilidad habilidad = habilidad(3L, "Retoque");
        perfil.getHabilidades().add(habilidad);
        usuarioActivo();
        perfilPropio(perfil);

        service.quitar(1L, 5L, 3L);

        assertFalse(perfil.getHabilidades().stream()
                .anyMatch(h -> h.getIdHabilidad().equals(3L)));
        verify(perfilRepository).save(perfil);
    }

    @Test
    void quitar_noAsociada_lanza409() {
        Perfil perfil = perfil(EstadoPerfil.Activo);
        usuarioActivo();
        perfilPropio(perfil);

        assertThrows(HabilidadNoAsociadaException.class,
                () -> service.quitar(1L, 5L, 3L));

        verify(perfilRepository, never()).save(any(Perfil.class));
    }

    @Test
    void quitar_perfilEnBaja_lanza409() {
        usuarioActivo();
        perfilPropio(perfil(EstadoPerfil.Baja));

        assertThrows(PerfilEnBajaException.class,
                () -> service.quitar(1L, 5L, 3L));
    }
}
