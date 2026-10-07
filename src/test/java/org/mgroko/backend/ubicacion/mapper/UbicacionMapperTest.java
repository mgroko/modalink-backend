package org.mgroko.backend.ubicacion.mapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.ubicacion.dto.CiudadResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

class UbicacionMapperTest {

    private static Pais pais(String iso, String nombre) {
        return Pais.builder().idPais(1L).codigoIso(iso).nombre(nombre).activo(true).build();
    }

    private static Provincia provincia(String nombre, String idExterno, Pais pais) {
        return Provincia.builder()
                .idProvincia(10L)
                .nombre(nombre)
                .idExterno(idExterno)
                .fuenteApi("GEOREF")
                .pais(pais)
                .build();
    }

    private static Ciudad ciudad(String nombre, String idExterno, Provincia provincia) {
        return Ciudad.builder()
                .idCiudad(100L)
                .nombre(nombre)
                .idExterno(idExterno)
                .fuenteApi("GEOREF")
                .provincia(provincia)
                .build();
    }

    @Test
    void toResponse_conTodaLaCadena_devuelveLaCadenaAnidada() {
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(7L)
                .direccion("Av. Corrientes 1234")
                .ciudad(ciudad("Recoleta", "0208401001",
                        provincia("Ciudad Autónoma de Buenos Aires", "02", pais("AR", "Argentina"))))
                .codigoPostal("C1024")
                .latitud(new BigDecimal("-34.588043854884"))
                .longitud(new BigDecimal("-58.3971817497302"))
                .build();

        UbicacionResponse response = UbicacionMapper.toResponse(ubicacion);

        assertEquals(7L, response.idUbicacion());
        assertEquals("Av. Corrientes 1234", response.direccion());
        assertEquals("C1024", response.codigoPostal());
        assertEquals(new BigDecimal("-34.588043854884"), response.latitud());
        assertEquals(new BigDecimal("-58.3971817497302"), response.longitud());

        assertEquals(100L, response.ciudad().idCiudad());
        assertEquals("Recoleta", response.ciudad().nombre());
        assertEquals("0208401001", response.ciudad().idExterno());
        assertEquals("GEOREF", response.ciudad().fuenteApi());

        assertEquals(10L, response.ciudad().provincia().idProvincia());
        assertEquals("Ciudad Autónoma de Buenos Aires", response.ciudad().provincia().nombre());
        assertEquals("02", response.ciudad().provincia().idExterno());
        assertEquals("GEOREF", response.ciudad().provincia().fuenteApi());

        assertEquals(1L, response.ciudad().provincia().pais().idPais());
        assertEquals("AR", response.ciudad().provincia().pais().codigoIso());
        assertEquals("Argentina", response.ciudad().provincia().pais().nombre());
    }

    @Test
    void toResponse_ubicacionNull_devuelveNull() {
        assertNull(UbicacionMapper.toResponse(null));
    }

    @Test
    void toResponse_sinCiudad_devuelveCiudadNull() {
        UbicacionResponse response = UbicacionMapper.toResponse(
                Ubicacion.builder().idUbicacion(8L).build());

        assertEquals(8L, response.idUbicacion());
        assertNull(response.ciudad());
        assertNull(response.codigoPostal());
        assertNull(response.latitud());
        assertNull(response.longitud());
    }

    @Test
    void toResponse_ciudadSinIdExternoNiPais_devuelveNullsEnLosEslabonesFaltantes() {
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(9L)
                .ciudad(ciudad("Saavedra", null, provincia("Buenos Aires", null, null)))
                .build();

        UbicacionResponse response = UbicacionMapper.toResponse(ubicacion);

        assertEquals("Saavedra", response.ciudad().nombre());
        assertNull(response.ciudad().idExterno());
        assertEquals("Buenos Aires", response.ciudad().provincia().nombre());
        assertNull(response.ciudad().provincia().idExterno());
        assertNull(response.ciudad().provincia().pais());
    }

    @Test
    void toResponse_ciudadSinProvincia_devuelveProvinciaNull() {
        UbicacionResponse response = UbicacionMapper.toResponse(
                Ubicacion.builder().idUbicacion(10L).ciudad(ciudad("Saavedra", "0208401002", null)).build());

        assertEquals("Saavedra", response.ciudad().nombre());
        assertNull(response.ciudad().provincia());
    }

    @Test
    void toCiudadResponse_exponeSoloLaCadenaDesdeLaCiudad() {
        CiudadResponse response = UbicacionMapper.toCiudadResponse(
                ciudad("La Plata", "06441030",
                        provincia("Buenos Aires", "06", pais("AR", "Argentina"))));

        assertEquals("La Plata", response.nombre());
        assertEquals("06441030", response.idExterno());
        assertEquals("Buenos Aires", response.provincia().nombre());
        assertEquals("Argentina", response.provincia().pais().nombre());
    }

    @Test
    void toCiudadResponse_null_devuelveNull() {
        assertNull(UbicacionMapper.toCiudadResponse(null));
    }
}
