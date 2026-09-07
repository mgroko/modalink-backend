package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UbicacionUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UbicacionService ubicacionService;

    @InjectMocks
    private UbicacionUsuarioService ubicacionUsuarioService;

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .apellido("Perez")
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Ubicacion ubicacionSaavedra(Long id) {
        return Ubicacion.builder()
                .idUbicacion(id)
                .idGeoref("0208401002")
                .localidad("Saavedra")
                .provincia("Ciudad Autónoma de Buenos Aires")
                .pais("Argentina")
                .latitud(new BigDecimal("-34.5548978526608"))
                .longitud(new BigDecimal("-58.4863271154338"))
                .build();
    }

    @Test
    void obtener_conUbicacion_devuelveRespuesta() {
        Usuario usuario = usuarioActivo();
        usuario.setUbicacion(ubicacionSaavedra(3L));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<UbicacionResponse> response = ubicacionUsuarioService.obtener(1L);

        assertTrue(response.isPresent());
        assertEquals("Saavedra", response.get().localidad());
        assertEquals("0208401002", response.get().localidadId());
    }

    @Test
    void obtener_sinUbicacion_devuelveVacio() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<UbicacionResponse> response = ubicacionUsuarioService.obtener(1L);

        assertTrue(response.isEmpty());
    }

    @Test
    void obtener_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> ubicacionUsuarioService.obtener(999L));
    }

    @Test
    void obtener_usuarioNoActivo_lanzaExcepcion() {
        Usuario deshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(deshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> ubicacionUsuarioService.obtener(1L));
    }

    @Test
    void asignar_localidadNueva_asociaUbicacion() {
        Usuario usuario = usuarioActivo();
        Ubicacion nueva = ubicacionSaavedra(10L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ubicacionService.obtenerOCrear("0208401002", null)).thenReturn(nueva);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UbicacionResponse response = ubicacionUsuarioService
                .asignar(1L, new UbicacionRequest("0208401002"));

        assertEquals("Saavedra", response.localidad());
        assertEquals("0208401002", response.localidadId());
        assertEquals(nueva, usuario.getUbicacion());
        verify(usuarioRepository).save(usuario);
        verify(ubicacionService).obtenerOCrear("0208401002", null);
    }

    @Test
    void asignar_reemplazaUbicacionPrevia() {
        Usuario usuario = usuarioActivo();
        usuario.setUbicacion(ubicacionSaavedra(3L));

        Ubicacion otra = Ubicacion.builder()
                .idUbicacion(4L)
                .idGeoref("06441030")
                .localidad("La Plata")
                .provincia("Buenos Aires")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ubicacionService.obtenerOCrear("06441030", null)).thenReturn(otra);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UbicacionResponse response = ubicacionUsuarioService
                .asignar(1L, new UbicacionRequest("06441030"));

        assertEquals(4L, response.idUbicacion());
        assertEquals(otra, usuario.getUbicacion());
    }

    @Test
    void asignar_localidadInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(ubicacionService.obtenerOCrear("9999999999", null))
                .thenThrow(new LocalidadNoEncontradaException("Localidad no encontrada."));

        assertThrows(LocalidadNoEncontradaException.class,
                () -> ubicacionUsuarioService.asignar(1L, new UbicacionRequest("9999999999")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void asignar_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> ubicacionUsuarioService.asignar(999L, new UbicacionRequest("0208401002")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void asignar_usuarioNoActivo_lanzaExcepcion() {
        Usuario deshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(deshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> ubicacionUsuarioService.asignar(1L, new UbicacionRequest("0208401002")));
    }

    @Test
    void quitar_desasociaUbicacion() {
        Usuario usuario = usuarioActivo();
        usuario.setUbicacion(ubicacionSaavedra(3L));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        ubicacionUsuarioService.quitar(1L);

        verify(usuarioRepository).save(usuario);
        assertNull(usuario.getUbicacion());
    }

    @Test
    void quitar_usuarioNoActivo_lanzaExcepcion() {
        Usuario pendiente = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.PendienteBaja)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(pendiente));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> ubicacionUsuarioService.quitar(1L));
    }
}