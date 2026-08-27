package org.mgroko.backend.usuario.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

class DatosPersonalesMapperTest {

    @Test
    void toResponse_conUbicacion_devuelveLocalidadProvincia() {
        Genero genero = mock(Genero.class);
        when(genero.getCodigo()).thenReturn("mujer");

        Ubicacion ubicacion = mock(Ubicacion.class);
        when(ubicacion.getLocalidad()).thenReturn("Buenos Aires");
        when(ubicacion.getProvincia()).thenReturn("CABA");

        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .fechaNacimiento(LocalDate.of(1990, 6, 15))
                .genero(genero)
                .ubicacion(ubicacion)
                .build();

        var response = DatosPersonalesMapper.toResponse(usuario);

        assertEquals(1L, response.idUsuario());
        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals(LocalDate.of(1990, 6, 15), response.fechaNacimiento());
        assertEquals("mujer", response.genero());
        assertEquals("Buenos Aires, CABA", response.ubicacion());
    }

    @Test
    void toResponse_sinUbicacion_devuelveNull() {
        Genero genero = mock(Genero.class);
        when(genero.getCodigo()).thenReturn("hombre");

        Usuario usuario = Usuario.builder()
                .idUsuario(2L)
                .nombre("Juan")
                .apellido("Perez")
                .fechaNacimiento(LocalDate.of(1985, 3, 10))
                .genero(genero)
                .ubicacion(null)
                .build();

        var response = DatosPersonalesMapper.toResponse(usuario);

        assertNull(response.ubicacion());
        assertEquals("Juan", response.nombre());
    }

    @Test
    void toResponse_generoNulo_devuelveNull() {
        Usuario usuario = Usuario.builder()
                .idUsuario(3L)
                .nombre("Ana")
                .apellido("Gomez")
                .fechaNacimiento(LocalDate.of(2000, 1, 1))
                .genero(null)
                .ubicacion(null)
                .build();

        var response = DatosPersonalesMapper.toResponse(usuario);

        assertNull(response.genero());
        assertNull(response.ubicacion());
        assertEquals("Ana", response.nombre());
    }
}
