package org.mgroko.backend.usuario.servicio;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mgroko.backend.usuario.dto.DatosPersonalesRequest;
import org.mgroko.backend.usuario.dto.DatosPersonalesResponse;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatosPersonalesServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private GeneroRepository generoRepository;

    @Mock
    private UbicacionService ubicacionService;

    @InjectMocks
    private DatosPersonalesService datosPersonalesService;

    private DatosPersonalesRequest requestValido() {
        return new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", null);
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .apellido("Perez")
                .estado(EstadoUsuario.Activo)
                .build();
    }

    @Test
    void actualizar_datosValidos_devuelveResponse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));

        Genero generoMujer = mock(Genero.class);
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DatosPersonalesResponse response = datosPersonalesService.actualizar(1L, requestValido());

        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals(LocalDate.of(1990, 6, 15), response.fechaNacimiento());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Maria", captor.getValue().getNombre());
        assertEquals("Flores", captor.getValue().getApellido());
    }

    @Test
    void actualizar_datosValidosConUbicacion_devuelveResponse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));

        Genero generoMujer = mock(Genero.class);
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));

        Ubicacion ubicacion = mock(Ubicacion.class);
        when(ubicacionService.obtenerOCrear("0208401002")).thenReturn(ubicacion);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DatosPersonalesRequest requestConUbicacion = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", "0208401002");

        DatosPersonalesResponse response = datosPersonalesService.actualizar(1L, requestConUbicacion);

        assertEquals("Maria", response.nombre());
        verify(usuarioRepository).save(any());
    }

    @Test
    void actualizar_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> datosPersonalesService.actualizar(999L, requestValido()));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_usuarioNoActivo_lanzaExcepcion() {
        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDeshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> datosPersonalesService.actualizar(1L, requestValido()));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_usuarioPendienteBaja_lanzaExcepcion() {
        Usuario usuarioPendiente = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.PendienteBaja)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioPendiente));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> datosPersonalesService.actualizar(1L, requestValido()));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_menorDeEdad_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));

        DatosPersonalesRequest requestMenor = new DatosPersonalesRequest(
                "Ana", "Gomez",
                LocalDate.now().minusYears(10),
                "mujer", null);

        assertThrows(EdadInvalidaException.class,
                () -> datosPersonalesService.actualizar(1L, requestMenor));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_generoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(generoRepository.findByCodigo("genero_inventado")).thenReturn(Optional.empty());

        DatosPersonalesRequest requestGeneroInvalido = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "genero_inventado", null);

        assertThrows(GeneroNoEncontradoException.class,
                () -> datosPersonalesService.actualizar(1L, requestGeneroInvalido));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_localidadInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));

        Genero generoMujer = mock(Genero.class);
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));

        when(ubicacionService.obtenerOCrear("999"))
                .thenThrow(new LocalidadNoEncontradaException("Localidad no encontrada."));

        DatosPersonalesRequest requestUbicacionInvalida = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", "999");

        assertThrows(LocalidadNoEncontradaException.class,
                () -> datosPersonalesService.actualizar(1L, requestUbicacionInvalida));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_localidadEnBlanco_noAsociaUbicacion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));

        Genero generoMujer = mock(Genero.class);
        when(generoRepository.findByCodigo("mujer")).thenReturn(Optional.of(generoMujer));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DatosPersonalesRequest requestUbicacionBlanca = new DatosPersonalesRequest(
                "Maria", "Flores",
                LocalDate.of(1990, 6, 15),
                "mujer", "  ");

        DatosPersonalesResponse response = datosPersonalesService.actualizar(1L, requestUbicacionBlanca);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        verify(ubicacionService, never()).obtenerOCrear(any());
        assertEquals("Maria", response.nombre());
    }
}
