package org.mgroko.backend.usuario.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

class DatosPersonalesMapperTest {

    private static Ubicacion ubicacionDe(String ciudad, String provincia, String pais) {
        Pais entidadPais = pais != null
                ? Pais.builder().idPais(1L).codigoIso("AR").nombre(pais).build()
                : null;
        Provincia entidadProvincia = provincia != null
                ? Provincia.builder().idProvincia(10L).nombre(provincia).idExterno("02")
                        .fuenteApi("GEOREF").pais(entidadPais).build()
                : null;
        return Ubicacion.builder()
                .idUbicacion(1L)
                .ciudad(Ciudad.builder()
                        .idCiudad(100L)
                        .nombre(ciudad)
                        .idExterno("0208401002")
                        .fuenteApi("GEOREF")
                        .provincia(entidadProvincia)
                        .build())
                .build();
    }

    @Test
    void toResponse_conUbicacion_devuelveLaUbicacionEstructurada() {
        Genero genero = mock(Genero.class);
        when(genero.getCodigo()).thenReturn("mujer");

        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .fechaNacimiento(LocalDate.of(1990, 6, 15))
                .genero(genero)
                .ubicacion(ubicacionDe("Buenos Aires", "Ciudad Autónoma de Buenos Aires", "Argentina"))
                .build();

        var response = DatosPersonalesMapper.toResponse(usuario);

        assertEquals(1L, response.idUsuario());
        assertEquals("Maria", response.nombre());
        assertEquals("Flores", response.apellido());
        assertEquals(LocalDate.of(1990, 6, 15), response.fechaNacimiento());
        assertEquals("mujer", response.genero());

        // Antes esto era el String "Buenos Aires, Ciudad Autónoma de Buenos Aires".
        assertEquals(1L, response.ubicacion().idUbicacion());
        assertEquals("Buenos Aires", response.ubicacion().ciudad().nombre());
        assertEquals("0208401002", response.ubicacion().ciudad().idExterno());
        assertEquals("Ciudad Autónoma de Buenos Aires",
                response.ubicacion().ciudad().provincia().nombre());
        assertEquals("Argentina", response.ubicacion().ciudad().provincia().pais().nombre());
    }

    @Test
    void toResponse_ciudadSinProvincia_noProduceComasSueltas() {
        Usuario usuario = Usuario.builder()
                .idUsuario(4L)
                .nombre("Lucia")
                .apellido("Rios")
                .ubicacion(ubicacionDe("Rosario", null, null))
                .build();

        var response = DatosPersonalesMapper.toResponse(usuario);

        // El mapper antiguo concatenaba "Rosario, null" sin querer.
        assertEquals("Rosario", response.ubicacion().ciudad().nombre());
        assertNull(response.ubicacion().ciudad().provincia());
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
